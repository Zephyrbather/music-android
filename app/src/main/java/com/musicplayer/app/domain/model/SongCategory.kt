package com.musicplayer.app.domain.model

object SongCategoryTags {
    val supported: List<String> = listOf(
        "Pop",
        "Rock",
        "Hip-Hop",
        "Electronic",
        "Classical",
        "Jazz",
        "Vocal",
        "Acoustic",
        "Bass",
        "Folk",
        "Metal",
        "Ambient",
        "Other"
    )

    val defaultPresetByCategory: Map<String, String> = mapOf(
        "Pop" to "pop",
        "Rock" to "rock",
        "Hip-Hop" to "bass",
        "Electronic" to "bass",
        "Classical" to "classical",
        "Jazz" to "vocal",
        "Vocal" to "vocal",
        "Acoustic" to "vocal",
        "Bass" to "bass",
        "Folk" to "vocal",
        "Metal" to "rock",
        "Ambient" to "flat",
        "Other" to "flat"
    )

    fun normalize(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return supported.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: "Other"
    }
}
