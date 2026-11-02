package com.example.konteksto.domain.model

data class HostSession(
    val id: String,
    val venue: String,
    val time: String,
    val talkCode: String,
    val title: String,
    val speakerName: String,
    val attendeesCount: Int,
    val isLive: Boolean = false,
    val bannerImageUrl: String? = null,
    val category: String? = null,
    val hostUid: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val status: String = "not_live",
)
