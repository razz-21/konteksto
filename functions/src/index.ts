import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenerativeAI, SchemaType } from "@google/generative-ai";
import * as crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

// Initialize the Google Gen AI SDK.
const ai = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || "");

// Cache version key: increment this whenever prompt or output schema changes to invalidate stale cache
const CACHE_VERSION = "v6_required_schema_fields";

// Helper to hash for caching
function getCacheHash(segmentIds: string[], language: string): string {
  const sortedIds = [...segmentIds].sort().join(",");
  const raw = `${sortedIds}_${language}_${CACHE_VERSION}`;
  return crypto.createHash("sha256").update(raw).digest("hex");
}

export const explainSelection = onCall({ region: "asia-southeast1" }, async (request) => {
  // 1. Auth & Validation
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be signed in.");
  }

  // Example for App Check enforcement (if enabled in Firebase, context.app is present)
  if (request.app === undefined) {
    // throw new HttpsError("failed-precondition", "The function must be called from an App Check verified app.");
  }

  const { sessionId, segmentIds, forceRefresh } = request.data;
  if (!sessionId || !Array.isArray(segmentIds) || segmentIds.length === 0 || segmentIds.length > 5) {
    throw new HttpsError("invalid-argument", "Invalid session ID or segment selection (max 5).");
  }

  const uid = request.auth.uid;

  // 2. Fetch User Profile to get language preference
  const userDoc = await db.collection("users").doc(uid).get();
  const explanationLanguage = userDoc.exists ? (userDoc.data()?.explanationLanguage || "english") : "english";

  // 3. Check cache (skip if forceRefresh is true)
  const cacheHash = getCacheHash(segmentIds, explanationLanguage);
  const cacheRef = db.collection(`sessions/${sessionId}/explanation_cache`).doc(cacheHash);

  if (!forceRefresh) {
    const cachedDoc = await cacheRef.get();
    if (cachedDoc.exists) {
      return cachedDoc.data();
    }
  }

  // 4. Rate Limiting (Basic example: max 10 requests per user per session)
  const rateLimitRef = db.collection(`sessions/${sessionId}/attendees/${uid}/rate_limits`).doc("explain");
  const rateLimitDoc = await rateLimitRef.get();
  const rateLimitData = rateLimitDoc.data() || { count: 0, resetAt: Date.now() + 3600000 };

  if (rateLimitData.count >= 15 && Date.now() < rateLimitData.resetAt) {
     throw new HttpsError("resource-exhausted", "Rate limit exceeded. Try again later.");
  }

  await rateLimitRef.set({
    count: (Date.now() >= rateLimitData.resetAt) ? 1 : (rateLimitData.count + 1),
    resetAt: (Date.now() >= rateLimitData.resetAt) ? Date.now() + 3600000 : rateLimitData.resetAt
  }, { merge: true });

  // 5. Gather Session Context
  const sessionDoc = await db.collection("sessions").doc(sessionId).get();
  if (!sessionDoc.exists) {
    throw new HttpsError("not-found", "Session not found.");
  }
  const sessionData = sessionDoc.data()!;

  // Verify participation (host or attendee)
  if (sessionData.hostUid !== uid) {
    const attendeeDoc = await db.collection(`sessions/${sessionId}/attendees`).doc(uid).get();
    if (!attendeeDoc.exists) {
        throw new HttpsError("permission-denied", "You must be part of the session.");
    }
  }

  // 6. Validate Segments & Fetch Preceding Context
  const transcriptsRef = db.collection(`sessions/${sessionId}/transcripts`);

  // Fetch all selected segments to verify they exist and get their timestamps
  const selectedDocs = await Promise.all(
      segmentIds.map(id => transcriptsRef.doc(id).get())
  );

  const validSelected = selectedDocs.filter((d: any) => d.exists).map((d: any) => d.data()!);
  if (validSelected.length !== segmentIds.length) {
      throw new HttpsError("not-found", "Some selected segments do not exist.");
  }

  // Sort selected segments by createdAt
  validSelected.sort((a: any, b: any) => a.createdAt - b.createdAt);

  // Check Adjacency: their createdAt indices or logic should ideally confirm no gaps,
  // but since we page by createdAt, we can just query the range between first and last
  // and see if count matches.
  const firstSelected = validSelected[0];
  const lastSelected = validSelected[validSelected.length - 1];

  const rangeSnapshot = await transcriptsRef
      .where("createdAt", ">=", firstSelected.createdAt)
      .where("createdAt", "<=", lastSelected.createdAt)
      .where("isSystemEvent", "==", false)
      .orderBy("createdAt")
      .get();

  if (rangeSnapshot.size !== validSelected.length) {
      throw new HttpsError("invalid-argument", "Selected segments are not strictly adjacent.");
  }

  // Fetch up to 5 preceding segments for context
  const precedingSnapshot = await transcriptsRef
      .where("createdAt", "<", firstSelected.createdAt)
      .orderBy("createdAt", "desc")
      .limit(5)
      .get();

  const precedingSegments = precedingSnapshot.docs.map(d => d.data()!).reverse();

  // 7. Fetch Host Context (Notes, Slides, Glossary)
  const notesSnapshot = await db.collection(`sessions/${sessionId}/context_notes`).get();
  const notes = notesSnapshot.docs.map(d => d.data());

  const slidesSnapshot = await db.collection(`sessions/${sessionId}/analyzed_slides`).get();
  const slides = slidesSnapshot.docs.map(d => d.data());

  const glossarySnapshot = await db.collection(`sessions/${sessionId}/domain_terms`).get();
  const glossary = glossarySnapshot.docs.map(d => d.data());

  // Set of valid context IDs for grounding validation
  const validContextIds = new Set<string>([
      ...validSelected.map(s => s.id),
      ...precedingSegments.map(s => s.id),
      ...notes.map(n => n.id),
      ...slides.map(s => s.id),
      ...glossary.map(g => g.id)
  ]);

  // 8. Build Prompt
  const prompt = `
You are an AI assistant helping a participant understand exactly what the speaker meant during a live presentation.

SESSION INFO:
Title: ${sessionData.title}
Category: ${sessionData.category}

<instructions>
1. Deep Contextual Breakdown (NOT a generic dictionary definition): Do NOT output a single generic textbook definition. Instead, unpack and elaborate on EVERYTHING the speaker explicitly mentioned in <selected_transcript>.
2. Detailed Itemization: If the speaker lists multiple items, types, keywords, or topics (e.g., private, default, protected, public modifiers), explain each of those specific items clearly using bullet points or sections.
3. Synthesize & Address ALL Selected Statements: Address and explain what the speaker meant across ALL ${validSelected.length} selected statement(s).
4. Output Language: Use the language: ${explanationLanguage}. If the language is 'taglish', mix Tagalog and English naturally, typical of a Philippine context.
5. Markdown Formatting: Structure the explanation thoroughly with Markdown (bold headers, bullet points, line breaks) so it is rich, clear, and easy to read.
6. Do NOT make up a meaning if the text is filler or vague (e.g., "okay so moving on"). If there is no substantive meaning to explain, set "explainable" to false.
7. Evidence Grounding: Provide evidence IDs in the "explanationGroundedIn" array. Use ONLY the IDs provided in the context tags below.
8. Context Integration: Base the intended meaning heavily on <host_notes>, <analyzed_slides>, and <glossary> if applicable, to enrich the explanation.
</instructions>

<preceding_transcript>
${precedingSegments.map(s => `[ID: ${s.id}] ${s.speakerName}: ${s.text}`).join("\n")}
</preceding_transcript>

<selected_transcript>
${validSelected.map((s, idx) => `[Statement ${idx + 1} - ID: ${s.id}] ${s.speakerName}: ${s.text}`).join("\n")}
</selected_transcript>

<host_notes>
${notes.map(n => `[ID: ${n.id}] ${n.title}: ${n.content}`).join("\n")}
</host_notes>

<glossary>
${glossary.map(g => `[ID: ${g.id}] ${g.term}: ${g.description}`).join("\n")}
</glossary>

<analyzed_slides>
${slides.map(s => `[ID: ${s.id}] Slide ${s.slideNumber}: ${s.title} - ${s.summary}`).join("\n")}
</analyzed_slides>
`;

  // 9. Define Output Schema
  const responseSchema = {
    type: SchemaType.OBJECT,
    properties: {
      explainable: {
        type: SchemaType.BOOLEAN,
        description: "True if the selected text contains coherent point(s) to explain, False if it's filler or vague."
      },
      type: {
        type: SchemaType.STRING,
        description: "A short category encompassing all selected statements (e.g., 'OOP & Access Modifiers')"
      },
      title: {
        type: SchemaType.STRING,
        description: "An overarching title summarizing what the speaker is presenting."
      },
      definition: {
        type: SchemaType.STRING,
        description: "An expanded, in-depth explanation of everything the speaker mentioned. Do NOT give a short textbook definition. Use Markdown formatting (bold headers, bullet points, line breaks) to break down each specific concept, keyword, modifier, or comparison the speaker brought up so the user gets a rich, thorough understanding."
      },
      explanationGroundedIn: {
        type: SchemaType.ARRAY,
        items: { type: SchemaType.STRING },
        description: "List of IDs (from transcript, notes, slides, glossary) that support the explanation."
      }
    },
    required: ["explainable", "type", "title", "definition", "explanationGroundedIn"]
  } as any;

  // 10. Call Gemini
  let modelResponse;
  try {
      const model = ai.getGenerativeModel({
          model: "gemini-2.5-flash",
          generationConfig: {
              responseMimeType: "application/json",
              responseSchema: responseSchema,
              temperature: 0.2
          }
      });
      const response = await model.generateContent(prompt);

      const responseText = response.response.text();
      if (!responseText) throw new Error("Empty response from model");

      modelResponse = JSON.parse(responseText);
  } catch (error) {
      console.error("Gemini call failed:", error);
      throw new HttpsError("internal", "Failed to generate explanation. Please try again.");
  }

  if (!modelResponse.explainable) {
      return {
          id: `unexplainable_${Date.now()}`,
          isExplainable: false,
          message: "The selected segment doesn't contain a clear concept to explain."
      };
  }

  // 11. Grounding Calculation
  const allClaimedIds = Array.isArray(modelResponse.explanationGroundedIn) ? modelResponse.explanationGroundedIn : [];

  const validClaimsCount = allClaimedIds.filter(id => validContextIds.has(id)).length;

  let percentGrounded = 100;
  if (allClaimedIds.length > 0) {
      percentGrounded = Math.round((validClaimsCount / allClaimedIds.length) * 100);
  } else {
      // If the model claimed no evidence but gave an explanation, we can penalize grounding or set it to 0.
      percentGrounded = 0;
  }

  // Calculate timestamp string
  const sessionStartMs = sessionData.createdAt || 0;
  const firstSegMs = firstSelected.createdAt || sessionStartMs;
  let elapsedMs = firstSegMs - sessionStartMs;
  if (elapsedMs < 0) elapsedMs = 0;

  const mins = Math.floor(elapsedMs / 60000);
  const secs = Math.floor((elapsedMs % 60000) / 1000);
  const timestampString = `At ${mins}m ${secs}s`;

  const finalExplanation = {
      id: `concept_${Date.now()}_${Math.floor(Math.random()*1000)}`,
      type: modelResponse.type || "Concept",
      title: modelResponse.title || "Unknown Concept",
      definition: modelResponse.definition || "",
      analogy: "",
      whyItMatters: "",
      timestamp: timestampString,
      percentGrounded: percentGrounded,
      isExplainable: true
  };

  // 12. Save to Cache
  await cacheRef.set(finalExplanation);

  return finalExplanation;
});
