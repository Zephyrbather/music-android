package com.musicplayer.app.domain.model

fun Song.artistGroupingId(): String {
    return artistId.takeUnless { it.isNullOrBlank() || it == "artist-unknown" }
        ?: stableMusicId("artist", artistName)
}

fun Song.albumGroupingId(): String {
    if (albumTitle.isUnresolvedAlbumTitle()) {
        return stableMusicId("album-unresolved", id.ifBlank { uri })
    }
    return albumId.takeUnless { it.isNullOrBlank() || it == "album-unknown" }
        ?: stableMusicId("album", "${artistName.trim()}|${albumTitle.trim()}")
}

fun stableMusicId(prefix: String, value: String): String {
    val normalized = value.lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "unknown" }
    return "$prefix-${Integer.toUnsignedString(normalized.hashCode(), 16)}"
}

fun formatReleaseDate(year: Int?, month: Int?): String? {
    val validYear = year?.takeIf { it > 0 } ?: return null
    val validMonth = month?.takeIf { it in 1..12 }
    return if (validMonth != null) {
        "$validYear-${validMonth.toString().padStart(2, '0')}"
    } else {
        validYear.toString()
    }
}

fun String.isUnresolvedAlbumTitle(): Boolean {
    val normalized = lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
    return normalized.isBlank() ||
        normalized == "unknown" ||
        normalized == "<unknown>" ||
        normalized == "unknown album" ||
        normalized == "folder import" ||
        normalized == "待刮削" ||
        normalized == "未知专辑" ||
        normalized == "不明专辑" ||
        normalized == "无专辑" ||
        normalized == "webdav" ||
        normalized == "web dav"
}

fun Song.hasUnresolvedAlbumTitle(): Boolean = albumTitle.isUnresolvedAlbumTitle()
