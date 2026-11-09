package com.example.konteksto.domain.model

/** The microphone the live transcript is recording from. */
data class AudioInputDevice(
    val type: AudioInputType,
    val productName: String = "",
)

enum class AudioInputType {
    BuiltInMic,
    WiredHeadset,
    Usb,
    Bluetooth,
}
