package com.example.konteksto.domain.model

/** The language an attendee wants AI explanations written in, saved on their profile. */
enum class ExplanationLanguage(val storageValue: String) {
    ENGLISH("english"),
    TAGALOG("tagalog"),
    TAGLISH("taglish"),
    ;

    companion object {
        val Default = ENGLISH

        /** Reads the value saved in Firestore or SharedPreferences, falling back to English for unknown values. */
        fun fromStorageValue(value: String?): ExplanationLanguage =
            entries.firstOrNull { it.storageValue.equals(value?.trim(), ignoreCase = true) } ?: Default
    }
}
