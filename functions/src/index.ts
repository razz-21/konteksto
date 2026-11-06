import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { GoogleGenerativeAI, SchemaType } from "@google/generative-ai";
import * as crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

// Initialize the Google Gen AI SDK.
const ai = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || "");

// Cache version key for explainSelection
const CACHE_VERSION = "v6_required_schema_fields";

function getCacheHash(segmentIds: string[], language: string): string {
  const sortedIds = [...segmentIds].sort().join(",");
  const raw = `${sortedIds}_${language}_${CACHE_VERSION}`;
  return crypto.createHash("sha256").update(raw).digest("hex");
}

function normalizeKey(str: string): string {
  return str
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, "")
    .trim()
    .replace(/\s+/g, " ");
}

// -------------------------------------------------------------------
// Feature E: Explain Selection Callable Function
// -------------------------------------------------------------------
export const explainSelection = onCall({ region: "asia-southeast1" }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be signed in.");
  }

  const { sessionId, segmentIds, forceRefresh } = request.data;
  if (!sessionId || !Array.isArray(segmentIds) || segmentIds.length === 0 || segmentIds.length > 5) {
    throw new HttpsError("invalid-argument", "Invalid session ID or segment selection (max 5).");
  }

  const uid = request.auth.uid;

  // 1. Fetch User Profile for language preference
  const userDoc = await db.collection("users").doc(uid).get();
  const explanationLanguage = userDoc.exists ? (userDoc.data()?.explanationLanguage || "english") : "english";

  // 2. Check cache
  const cacheHash = getCacheHash(segmentIds, explanationLanguage);
  const cacheRef = db.collection(`sessions/${sessionId}/explanation_cache`).doc(cacheHash);

  if (!forceRefresh) {
    const cachedDoc = await cacheRef.get();
    if (cachedDoc.exists) {
      return cachedDoc.data();
    }
  }

  // 3. Rate Limiting (max 15 requests per hour)
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

  // 4. Session Context
  const sessionDoc = await db.collection("sessions").doc(sessionId).get();
  if (!sessionDoc.exists) {
    throw new HttpsError("not-found", "Session not found.");
  }
  const sessionData = sessionDoc.data()!;

  const transcriptsRef = db.collection(`sessions/${sessionId}/transcripts`);

  const selectedDocs = await Promise.all(
    segmentIds.map(id => transcriptsRef.doc(id).get())
  );

  const validSelected = selectedDocs.filter((d: any) => d.exists).map((d: any) => d.data()!);
  if (validSelected.length !== segmentIds.length) {
    throw new HttpsError("not-found", "Some selected segments do not exist.");
  }

  validSelected.sort((a: any, b: any) => a.createdAt - b.createdAt);

  const firstSelected = validSelected[0];

  const precedingSnapshot = await transcriptsRef
    .where("createdAt", "<", firstSelected.createdAt)
    .orderBy("createdAt", "desc")
    .limit(5)
    .get();

  const precedingSegments = precedingSnapshot.docs.map(d => d.data()!).reverse();

  const notesSnapshot = await db.collection(`sessions/${sessionId}/context_notes`).get();
  const notes = notesSnapshot.docs.map(d => d.data());

  const slidesSnapshot = await db.collection(`sessions/${sessionId}/analyzed_slides`).get();
  const slides = slidesSnapshot.docs.map(d => d.data());

  const glossarySnapshot = await db.collection(`sessions/${sessionId}/domain_terms`).get();
  const glossary = glossarySnapshot.docs.map(d => d.data());

  const validContextIds = new Set<string>([
    ...validSelected.map((s: any) => s.id),
    ...precedingSegments.map((s: any) => s.id),
    ...notes.map((n: any) => n.id),
    ...slides.map((s: any) => s.id),
    ...glossary.map((g: any) => g.id)
  ]);

  const prompt = `
You are an AI assistant helping a participant understand exactly what the speaker meant during a live presentation.

SESSION INFO:
Title: ${sessionData.title}
Category: ${sessionData.category}

<instructions>
1. Deep Contextual Breakdown: Do NOT output a single generic textbook definition. Unpack and elaborate on EVERYTHING the speaker explicitly mentioned in <selected_transcript>.
2. Detailed Itemization: If the speaker lists multiple items, types, or keywords, explain each clearly using bullet points or sections.
3. Synthesize & Address ALL Selected Statements: Address and explain what the speaker meant across ALL ${validSelected.length} selected statement(s).
4. Output Language: Use the language: ${explanationLanguage}. If 'taglish', mix Tagalog and English naturally.
5. Markdown Formatting: Structure thoroughly with Markdown (bold headers, bullet points, line breaks).
6. Evidence Grounding: Provide evidence IDs in the "explanationGroundedIn" array using ONLY the IDs provided in context tags below.
</instructions>

<preceding_transcript>
${precedingSegments.map((s: any) => `[ID: ${s.id}] ${s.speakerName}: ${s.text}`).join("\n")}
</preceding_transcript>

<selected_transcript>
${validSelected.map((s: any, idx: number) => `[Statement ${idx + 1} - ID: ${s.id}] ${s.speakerName}: ${s.text}`).join("\n")}
</selected_transcript>

<host_notes>
${notes.map((n: any) => `[ID: ${n.id}] ${n.title}: ${n.content}`).join("\n")}
</host_notes>

<glossary>
${glossary.map((g: any) => `[ID: ${g.id}] ${g.term}: ${g.description}`).join("\n")}
</glossary>

<analyzed_slides>
${slides.map((s: any) => `[ID: ${s.id}] Slide ${s.slideNumber}: ${s.title} - ${s.summary}`).join("\n")}
</analyzed_slides>
`;

  const responseSchema = {
    type: SchemaType.OBJECT,
    properties: {
      explainable: {
        type: SchemaType.BOOLEAN,
        description: "True if the selected text contains coherent point(s) to explain, False if filler."
      },
      type: {
        type: SchemaType.STRING,
        description: "A short category encompassing selected statements"
      },
      title: {
        type: SchemaType.STRING,
        description: "An overarching title summarizing what the speaker presented."
      },
      definition: {
        type: SchemaType.STRING,
        description: "An expanded, in-depth explanation using Markdown formatting."
      },
      explanationGroundedIn: {
        type: SchemaType.ARRAY,
        items: { type: SchemaType.STRING },
        description: "List of IDs supporting the explanation."
      }
    },
    required: ["explainable", "type", "title", "definition", "explanationGroundedIn"]
  } as any;

  let modelResponse: any;
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

  const allClaimedIds: string[] = Array.isArray(modelResponse.explanationGroundedIn) ? modelResponse.explanationGroundedIn : [];
  const validClaimsCount = allClaimedIds.filter((id: string) => validContextIds.has(id)).length;

  let percentGrounded = 100;
  if (allClaimedIds.length > 0) {
    percentGrounded = Math.round((validClaimsCount / allClaimedIds.length) * 100);
  } else {
    percentGrounded = 0;
  }

  const sessionStartMs = sessionData.createdAt || 0;
  const firstSegMs = firstSelected.createdAt || sessionStartMs;
  let elapsedMs = firstSegMs - sessionStartMs;
  if (elapsedMs < 0) elapsedMs = 0;

  const mins = Math.floor(elapsedMs / 60000);
  const secs = Math.floor((elapsedMs % 60000) / 1000);
  const timestampString = `At ${mins}m ${secs}s`;

  const finalExplanation = {
    id: `concept_${Date.now()}_${Math.floor(Math.random() * 1000)}`,
    type: modelResponse.type || "Concept",
    title: modelResponse.title || "Unknown Concept",
    definition: modelResponse.definition || "",
    analogy: "",
    whyItMatters: "",
    timestamp: timestampString,
    percentGrounded: percentGrounded,
    isExplainable: true
  };

  await cacheRef.set(finalExplanation);
  return finalExplanation;
});


// -------------------------------------------------------------------
// Feature F: AI Terminology & Glossary Engine (Batch Functions)
// -------------------------------------------------------------------

/**
 * Core function to run Gemini glossary extraction on a batch of unprocessed transcript segments.
 */
async function runGlossaryExtractionBatch(sessionId: string): Promise<number> {
  const sessionDoc = await db.collection("sessions").doc(sessionId).get();
  if (!sessionDoc.exists) return 0;
  const sessionData = sessionDoc.data()!;

  // 1. Fetch cursor state
  const stateRef = db.collection(`sessions/${sessionId}/glossary_engine`).doc("state");
  const stateDoc = await stateRef.get();
  const lastProcessedCreatedAt = stateDoc.exists ? (stateDoc.data()?.lastProcessedCreatedAt || 0) : 0;

  // 2. Fetch new unprocessed transcript segments
  const newTranscriptsSnapshot = await db.collection(`sessions/${sessionId}/transcripts`)
    .where("createdAt", ">", lastProcessedCreatedAt)
    .where("isSystemEvent", "==", false)
    .orderBy("createdAt", "asc")
    .limit(30)
    .get();

  if (newTranscriptsSnapshot.empty) return 0;

  const newSegments = newTranscriptsSnapshot.docs.map(d => d.data());
  const newestCreatedAt = newSegments[newSegments.length - 1].createdAt;

  // 3. Fetch context notes, slide summaries, and existing glossary terms
  const [notesSnap, slidesSnap, glossarySnap, domainTermsSnap] = await Promise.all([
    db.collection(`sessions/${sessionId}/context_notes`).get(),
    db.collection(`sessions/${sessionId}/analyzed_slides`).get(),
    db.collection(`sessions/${sessionId}/glossary`).get(),
    db.collection(`sessions/${sessionId}/domain_terms`).get()
  ]);

  const notes = notesSnap.docs.map(d => d.data());
  const slides = slidesSnap.docs.map(d => d.data());
  const existingGlossary = glossarySnap.docs.map(d => d.data());
  const existingDomainTerms = domainTermsSnap.docs.map(d => d.data());

  // Set of known keys that shouldn't be re-generated or overwritten
  const protectedKeys = new Set<string>();
  existingGlossary.forEach((item: any) => {
    if (item.hostEdited || item.hidden || item.source === "slides") {
      if (item.normalizedKey) protectedKeys.add(item.normalizedKey);
      if (item.term) protectedKeys.add(normalizeKey(item.term));
      if (Array.isArray(item.aliases)) {
        item.aliases.forEach((a: string) => protectedKeys.add(normalizeKey(a)));
      }
    }
  });

  existingDomainTerms.forEach((item: any) => {
    if (item.term) protectedKeys.add(normalizeKey(item.term));
  });

  // 4. Construct prompt for Gemini
  const prompt = `
You are an expert AI glossary engine listening to a live seminar / conference talk.
Session Title: "${sessionData.title}" (${sessionData.category || "General"})

<host_notes>
${notes.map((n: any) => `- ${n.title}: ${n.content}`).join("\n")}
</host_notes>

<analyzed_slides>
${slides.map((s: any) => `- Slide ${s.slideNumber}: ${s.title} — ${s.summary}`).join("\n")}
</analyzed_slides>

<known_terms>
${existingGlossary.map((g: any) => `- ${g.term}`).join("\n")}
</known_terms>

<new_live_transcript_segments>
${newSegments.map((s: any) => `[Segment ID: ${s.id}] ${s.speakerName}: ${s.text}`).join("\n")}
</new_live_transcript_segments>

TASK:
Analyze the new live transcript segments above. Identify new, unfamiliar technical terms, acronyms, abbreviations, or domain jargon spoken by the presenter that are NOT in <known_terms>.

RULES:
- Return ONLY new terms that are genuinely relevant to this talk.
- For abbreviations/acronyms, include full form e.g. "POP (Point of Presence)".
- Category MUST be one of: "ABBREVIATION", "TECHNICAL TERM", or "JARGON".
- Definition MUST be 1-2 plain language sentences specific to this talk context.
- If unsure of a term's meaning, leave it out. Do not guess.
`;

  const glossarySchema = {
    type: SchemaType.OBJECT,
    properties: {
      terms: {
        type: SchemaType.ARRAY,
        items: {
          type: SchemaType.OBJECT,
          properties: {
            term: { type: SchemaType.STRING, description: "Term name" },
            aliases: {
              type: SchemaType.ARRAY,
              items: { type: SchemaType.STRING },
              description: "Aliases or alternate forms"
            },
            category: {
              type: SchemaType.STRING,
              description: "ABBREVIATION, TECHNICAL TERM, or JARGON"
            },
            definition: {
              type: SchemaType.STRING,
              description: "1-2 sentence plain language definition"
            },
            firstSegmentId: {
              type: SchemaType.STRING,
              description: "ID of the segment where this term first appeared"
            }
          },
          required: ["term", "aliases", "category", "definition"]
        }
      }
    },
    required: ["terms"]
  } as any;

  let termsResult: any[] = [];
  try {
    const model = ai.getGenerativeModel({
      model: "gemini-2.5-flash",
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: glossarySchema,
        temperature: 0.2
      }
    });

    const response = await model.generateContent(prompt);
    const text = response.response.text();
    if (text) {
      const parsed = JSON.parse(text);
      termsResult = Array.isArray(parsed.terms) ? parsed.terms : [];
    }
  } catch (err) {
    console.error("Glossary engine Gemini generation failed:", err);
    return 0;
  }

  if (termsResult.length === 0) {
    // Update cursor state even if no new terms found
    await stateRef.set({
      unprocessedCount: 0,
      lastProcessedAt: Date.now(),
      lastProcessedCreatedAt: newestCreatedAt
    }, { merge: true });
    return 0;
  }

  // 5. Deduplicate and Save terms to Firestore
  const batch = db.batch();
  let addedCount = 0;

  for (const item of termsResult) {
    const rawTerm = (item.term || "").trim();
    if (!rawTerm) continue;

    const nKey = normalizeKey(rawTerm);
    if (protectedKeys.has(nKey)) continue;

    const termId = `term_${nKey.replace(/\s+/g, "_")}`;
    const glossaryRef = db.collection(`sessions/${sessionId}/glossary`).doc(termId);
    const domainTermRef = db.collection(`sessions/${sessionId}/domain_terms`).doc(termId);

    const firstSeg = item.firstSegmentId || newSegments[0]?.id || "";

    const termData = {
      termId: termId,
      term: rawTerm,
      normalizedKey: nKey,
      aliases: Array.isArray(item.aliases) ? item.aliases : [],
      category: item.category || "TECHNICAL TERM",
      definition: item.definition || "",
      source: "transcript",
      firstSegmentId: firstSeg,
      hostEdited: false,
      hidden: false,
      timestamp: Date.now()
    };

    const domainTermData = {
      id: termId,
      term: rawTerm,
      category: item.category || "TECHNICAL TERM",
      description: item.definition || "",
      updatedAt: Date.now()
    };

    batch.set(glossaryRef, termData, { merge: true });
    batch.set(domainTermRef, domainTermData, { merge: true });

    protectedKeys.add(nKey);
    addedCount++;
  }

  // Update cursor state
  batch.set(stateRef, {
    unprocessedCount: 0,
    lastProcessedAt: Date.now(),
    lastProcessedCreatedAt: newestCreatedAt
  }, { merge: true });

  await batch.commit();
  console.log(`Glossary Engine added ${addedCount} new terms for session ${sessionId}`);
  return addedCount;
}

/**
 * Firestore Trigger: Runs when a new transcript segment is created.
 * Uses batching: triggers Gemini when unprocessed count >= 8 OR 30 seconds since last run.
 */
export const onTranscriptCreated = onDocumentCreated(
  {
    document: "sessions/{sessionId}/transcripts/{segmentId}",
    region: "asia-southeast1"
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data();
    if (data.isSystemEvent) return;

    const sessionId = event.params.sessionId;
    const stateRef = db.collection(`sessions/${sessionId}/glossary_engine`).doc("state");

    let shouldRunBatch = false;

    await db.runTransaction(async (transaction) => {
      const stateDoc = await transaction.get(stateRef);
      const state = stateDoc.data() || { unprocessedCount: 0, lastProcessedAt: 0, lastProcessedCreatedAt: 0 };

      const newCount = state.unprocessedCount + 1;
      const now = Date.now();
      const timeElapsed = now - state.lastProcessedAt;

      if (newCount >= 8 || timeElapsed >= 30000) {
        shouldRunBatch = true;
        transaction.set(stateRef, {
          unprocessedCount: 0,
          lastProcessedAt: now,
          lastProcessedCreatedAt: state.lastProcessedCreatedAt
        }, { merge: true });
      } else {
        transaction.set(stateRef, {
          unprocessedCount: newCount
        }, { merge: true });
      }
    });

    if (shouldRunBatch) {
      await runGlossaryExtractionBatch(sessionId);
    }
  }
);

/**
 * Firestore Trigger: Runs when session status changes (e.g. paused or ended) to perform a final pass.
 */
export const onSessionStatusChanged = onDocumentUpdated(
  {
    document: "sessions/{sessionId}",
    region: "asia-southeast1"
  },
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();
    if (!beforeData || !afterData) return;

    const sessionId = event.params.sessionId;
    const oldStatus = beforeData.status;
    const newStatus = afterData.status;

    if (oldStatus !== newStatus && (newStatus === "paused" || newStatus === "ended")) {
      console.log(`Session ${sessionId} changed status to ${newStatus}. Running final glossary pass...`);
      await runGlossaryExtractionBatch(sessionId);
    }
  }
);

/**
 * Callable Function: Allows manual trigger of glossary pass for a session.
 */
export const triggerGlossaryPass = onCall({ region: "asia-southeast1" }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be signed in.");
  }
  const { sessionId } = request.data;
  if (!sessionId) {
    throw new HttpsError("invalid-argument", "Session ID required.");
  }

  const addedCount = await runGlossaryExtractionBatch(sessionId);
  return { success: true, addedTermsCount: addedCount };
});
