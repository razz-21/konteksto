package com.example.konteksto.ui.host.studio

data class TranscriptEntry(
    val id: String,
    val speakerName: String,
    val timestamp: String,
    val text: String,
)

data class ContextNoteItem(
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val timestamp: String,
)
