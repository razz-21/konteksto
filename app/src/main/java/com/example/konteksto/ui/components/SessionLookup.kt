package com.example.konteksto.ui.components

import com.example.konteksto.domain.model.HostSession

/** Where a screen is in loading the one session it shows. */
sealed interface SessionLookup {
    data object Loading : SessionLookup
    data class Found(val session: HostSession) : SessionLookup
    data object NotFound : SessionLookup
    data object Failed : SessionLookup
}
