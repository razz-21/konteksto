package com.example.konteksto.domain.model

data class LiveConceptExplanationItem(
    val id: String,
    val type: String = "Core Neural Mechanics",
    val timestamp: String = "At 14m 20s",
    val percentGrounded: Int = 98,
    val title: String = "Self-Attention Weights",
    val definition: String = "Mathematical scores calculated by the model that determine how much focus or weight each word gives to other words in the sentence.",
    val analogy: String = "Like a highlighter pen that dynamically emphasizes the pronouns and their antecedents while you read.",
    val whyItMatters: String = "Without self-attention, the model would treat words in isolation and miss contextual nuance or sentence-level logic.",
    val isBookmarked: Boolean = false,
    /** Server time the concept engine saved it; 0 for explanations that are never saved. */
    val createdAt: Long = 0L,
)
