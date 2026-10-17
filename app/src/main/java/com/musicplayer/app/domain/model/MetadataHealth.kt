package com.musicplayer.app.domain.model

enum class MetadataIssueType {
    SWAPPED_NAMES,
    UUID_ALBUM,
    INVALID_TEXT,
    DUPLICATE
}

data class MetadataIssue(
    val type: MetadataIssueType,
    val songId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val proposedTitle: String,
    val proposedArtist: String,
    val proposedAlbum: String,
    val duplicateIds: List<String> = emptyList()
) {
    val duplicateCount: Int get() = if (duplicateIds.isEmpty()) 1 else duplicateIds.size + 1
}

fun detectMetadataIssues(
    songs: List<Song>,
    fallbackTitle: String,
    fallbackArtist: String,
    fallbackAlbum: String
): List<MetadataIssue> {
    if (songs.isEmpty()) return emptyList()
    val issues = mutableListOf<MetadataIssue>()
    val titleIndex = songs.map { it.title.normalizedForDuplicateMatch() }
        .filter { it.length >= 2 }
        .toSet()
    val artistIndex = songs.map { it.artistName.normalizedForDuplicateMatch() }
        .filter { it.length >= 2 }
        .toSet()

    songs.forEach { song ->
        val lyrics = song.lyrics.orEmpty()
        val lyricTitle = lyricTagValue(lyrics, "ti")
        val lyricArtist = lyricTagValue(lyrics, "ar")

        var proposedTitle = song.title
        var proposedArtist = song.artistName
        var proposedAlbum = song.albumTitle

        val normalizedTitle = song.title.normalizedForDuplicateMatch()
        val normalizedArtist = song.artistName.normalizedForDuplicateMatch()
        val lyricSwap = lyricTitle != null && lyricArtist != null &&
            normalizedTitle == lyricArtist.normalizedForDuplicateMatch() &&
            normalizedArtist == lyricTitle.normalizedForDuplicateMatch() &&
            !lyricTitle.equals(lyricArtist, ignoreCase = true)
        val librarySwap = normalizedTitle != normalizedArtist &&
            normalizedTitle in artistIndex &&
            normalizedArtist in titleIndex
        val swapped = lyricSwap || librarySwap
        if (swapped) {
            proposedTitle = song.artistName
            proposedArtist = song.title
        }

        val cleanedTitle = proposedTitle.cleanMetadataText(fallbackTitle)
        val cleanedArtist = proposedArtist.cleanMetadataText(fallbackArtist)
        val cleanedAlbum = proposedAlbum.cleanMetadataText(fallbackAlbum)
        val invalidText = cleanedTitle != proposedTitle ||
            cleanedArtist != proposedArtist ||
            cleanedAlbum != proposedAlbum
        proposedTitle = cleanedTitle
        proposedArtist = cleanedArtist
        proposedAlbum = cleanedAlbum

        val uuidAlbum = proposedAlbum.looksLikeUuid()
        if (uuidAlbum) {
            proposedAlbum = songs.asSequence()
                .filter { it.id != song.id }
                .filter { !it.albumTitle.looksLikeUuid() && !it.albumTitle.isUnresolvedAlbumTitle() }
                .filter {
                    it.title.normalizedForDuplicateMatch() == normalizedTitle &&
                        it.artistName.normalizedForDuplicateMatch() == normalizedArtist
                }
                .map { it.albumTitle }
                .firstOrNull()
                ?: fallbackAlbum
        }

        if (swapped) {
            issues += MetadataIssue(
                type = MetadataIssueType.SWAPPED_NAMES,
                songId = song.id,
                title = song.title,
                artistName = song.artistName,
                albumTitle = song.albumTitle,
                proposedTitle = proposedTitle,
                proposedArtist = proposedArtist,
                proposedAlbum = proposedAlbum
            )
        }
        if (uuidAlbum) {
            issues += MetadataIssue(
                type = MetadataIssueType.UUID_ALBUM,
                songId = song.id,
                title = song.title,
                artistName = song.artistName,
                albumTitle = song.albumTitle,
                proposedTitle = proposedTitle,
                proposedArtist = proposedArtist,
                proposedAlbum = proposedAlbum
            )
        }
        if (invalidText) {
            issues += MetadataIssue(
                type = MetadataIssueType.INVALID_TEXT,
                songId = song.id,
                title = song.title,
                artistName = song.artistName,
                albumTitle = song.albumTitle,
                proposedTitle = proposedTitle,
                proposedArtist = proposedArtist,
                proposedAlbum = proposedAlbum
            )
        }
    }

    issues += detectDuplicates(songs)
    return issues
}

private fun lyricTagValue(lyrics: String, tag: String): String? {
    return Regex("\\[$tag:([^\\]]*)]", RegexOption.IGNORE_CASE)
        .find(lyrics)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun detectDuplicates(songs: List<Song>): List<MetadataIssue> {
    val groups = songs.groupBy { song ->
        "${song.title.normalizedForDuplicateMatch()}\u0000${song.artistName.normalizedForDuplicateMatch()}"
    }
    val issues = mutableListOf<MetadataIssue>()
    groups.forEach { (key, group) ->
        val parts = key.split('\u0000')
        if (group.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return@forEach
        val clusters = mutableListOf<MutableList<Song>>()
        group.sortedBy { it.durationMs }.forEach { song ->
            val cluster = clusters.lastOrNull()
            if (cluster != null && song.durationMs - cluster.last().durationMs <= 5_000L) {
                cluster += song
            } else {
                clusters += mutableListOf(song)
            }
        }
        clusters.filter { it.size >= 2 }.forEach { cluster ->
            val keeper = cluster.maxWith(
                compareBy<Song> { it.completenessScore() }
                    .thenBy { (it.bitrateKbps ?: 0) + (it.sampleRateHz ?: 0) / 1000 }
                    .thenByDescending { it.dateAddedEpochMs }
            )
            issues += MetadataIssue(
                type = MetadataIssueType.DUPLICATE,
                songId = keeper.id,
                title = keeper.title,
                artistName = keeper.artistName,
                albumTitle = keeper.albumTitle,
                proposedTitle = keeper.title,
                proposedArtist = keeper.artistName,
                proposedAlbum = keeper.albumTitle,
                duplicateIds = cluster.filter { it.id != keeper.id }.map { it.id }
            )
        }
    }
    return issues
}

private fun Song.completenessScore(): Int {
    var score = 0
    if (!artworkUri.isNullOrBlank()) score += 2
    if (!lyrics.isNullOrBlank()) score += 2
    if (releaseYear != null) score += 1
    if (!albumTitle.isUnresolvedAlbumTitle() && !albumTitle.looksLikeUuid()) score += 1
    if (bitrateKbps != null || sampleRateHz != null) score += 1
    if (source == Source.LOCAL) score += 1
    return score
}
