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

internal fun ContextNoteItem.withEdits(title: String, content: String): ContextNoteItem = copy(
    title = title.trim(),
    content = content.trim(),
)

data class AnalyzedSlideItem(
    val id: String,
    val slideNumber: Int,
    val title: String,
    val summary: String? = null,
    val explanation: String? = null,
    val keyTakeaway: String? = null,
)

internal fun AnalyzedSlideItem.withEdits(
    title: String,
    summary: String,
    explanation: String,
    keyTakeaway: String,
): AnalyzedSlideItem = copy(
    title = title.trim(),
    summary = summary.trim().ifBlank { null },
    explanation = explanation.trim().ifBlank { null },
    keyTakeaway = keyTakeaway.trim().ifBlank { null },
)

data class SlideDeckInfo(
    val fileName: String = "Neural_Architectures_Key.pdf",
    val fileSizeMb: Double = 14.2,
    val pageCount: Int = 1,
    val uploadTime: String = "Uploaded Today, 08:45 AM",
    val isAnalyzed: Boolean = true,
    val pdfBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlideDeckInfo

        if (fileName != other.fileName) return false
        if (fileSizeMb != other.fileSizeMb) return false
        if (pageCount != other.pageCount) return false
        if (uploadTime != other.uploadTime) return false
        if (isAnalyzed != other.isAnalyzed) return false
        if (pdfBytes != null) {
            if (other.pdfBytes == null) return false
            if (!pdfBytes.contentEquals(other.pdfBytes)) return false
        } else if (other.pdfBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileSizeMb.hashCode()
        result = 31 * result + pageCount.hashCode()
        result = 31 * result + uploadTime.hashCode()
        result = 31 * result + isAnalyzed.hashCode()
        result = 31 * result + (pdfBytes?.contentHashCode() ?: 0)
        return result
    }
}
