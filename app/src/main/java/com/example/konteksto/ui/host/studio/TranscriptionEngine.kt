package com.example.konteksto.ui.host.studio

import kotlinx.coroutines.flow.StateFlow

/** Turns the host's speech into text, reporting each finished segment to whoever created the engine. */
interface TranscriptionEngine {
    val partialText: StateFlow<String>
    fun startListening()

    /** Stops listening and reports any words still in progress as a finished segment. */
    fun stopListening()
}
