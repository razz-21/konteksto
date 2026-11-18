package com.example.konteksto.ui.host.studio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** What turns the host's speech into text, shown in the live transcript header. */
enum class TranscriptionEngineType { Gemini, PhoneRecognizer }

private val alwaysPhoneRecognizer = MutableStateFlow(TranscriptionEngineType.PhoneRecognizer).asStateFlow()

/** Turns the host's speech into text, reporting each finished segment to whoever created the engine. */
interface TranscriptionEngine {
    val partialText: StateFlow<String>

    /** The engine in use, or the one the next broadcast will try first. */
    val activeEngine: StateFlow<TranscriptionEngineType> get() = alwaysPhoneRecognizer

    /** Things the host should be told about, such as the engine switching to a fallback. */
    val messages: Flow<String> get() = emptyFlow()

    fun startListening()

    /** Stops listening and reports any words still in progress as a finished segment. */
    fun stopListening()
}
