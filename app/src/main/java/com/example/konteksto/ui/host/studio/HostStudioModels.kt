package com.example.konteksto.ui.host.studio

data class TranscriptEntry(
    val id: String,
    val speakerName: String,
    val timestamp: String,
    val text: String,
    val isSystemEvent: Boolean = false,
)

data class ContextNoteItem(
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val timestamp: String,
)

data class AnalyzedSlideItem(
    val id: String,
    val slideNumber: Int,
    val title: String,
    val diagramSummary: String? = null,
    val keyTakeaway: String? = null,
)

data class SlideDeckInfo(
    val fileName: String = "Neural_Architectures_Key.pdf",
    val fileSizeMb: Double = 14.2,
    val pageCount: Int = 1,
    val uploadTime: String = "Uploaded Today, 08:45 AM",
    val isAnalyzed: Boolean = true,
)
