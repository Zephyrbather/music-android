package com.musicplayer.app.data.scraper

import com.musicplayer.app.data.cache.CacheManager
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.ScrapeCache
import com.musicplayer.app.domain.model.isUnresolvedAlbumTitle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

interface MetadataScraper {
    suspend fun enrich(song: Song, provider: MetadataProvider = MetadataProvider.AUTO): Song
    suspend fun findArtistArtwork(artistName: String): ArtistArtworkResult?
}

@Singleton
class CompositeMetadataScraper @Inject constructor(
    private val sidecarMetadataScraper: SidecarMetadataScraper,
    private val musicBrainzScraper: MusicBrainzScraper,
    private val coverArtArchiveScraper: CoverArtArchiveScraper,
    private val lrcLibScraper: LrcLibScraper,
    private val netEaseScraper: NetEaseScraper,
    private val megalobizScraper: MegalobizScraper,
    private val lyricsOvhScraper: LyricsOvhScraper,
    private val iTunesScraper: ITunesScraper,
    private val deezerScraper: DeezerScraper,
    private val geniusScraper: GeniusScraper,
    private val jioSaavnScraper: JioSaavnScraper,
    private val qqMusicScraper: QQMusicScraper,
    private val kuGouScraper: KuGouScraper,
    private val kuwoScraper: KuwoScraper,
    private val miguScraper: MiguScraper,
    private val cacheManager: CacheManager
) : MetadataScraper {
    override suspend fun enrich(song: Song, provider: MetadataProvider): Song {
        val normalized = musicBrainzScraper.normalize(song)
        val localFirst = sidecarMetadataScraper.enrich(normalized)
        if (provider == MetadataProvider.AUTO) {
            return enrichMissingAutomatically(localFirst)
        }
        val online = coroutineScope {
            val lyrics = async {
                if (localFirst.lyrics.isNullOrBlank()) lyricsByProvider(localFirst, provider) else null
            }
            val artwork = async {
                if (localFirst.artworkUri.isNullOrBlank()) artworkByProvider(localFirst, provider) else null
            }
            lyrics.await() to artwork.await()
        }

        return localFirst.copy(
            lyrics = localFirst.lyrics ?: online.first?.lyrics,
            lyricsProvider = localFirst.lyricsProvider ?: online.first?.provider,
            artworkUri = localFirst.artworkUri ?: online.second?.imageUri,
            artworkProvider = localFirst.artworkProvider ?: online.second?.provider
        )
    }

    private suspend fun enrichMissingAutomatically(song: Song): Song {
        val missingLyrics = song.lyrics.isNullOrBlank()
        val missingArtwork = song.artworkUri.isNullOrBlank()
        val missingReleaseDate = song.releaseYear == null
        val missingAlbum = song.albumTitle.isUnresolvedAlbumTitle()
        return when {
            missingLyrics && missingArtwork -> {
                autoFullMetadataCandidate(song)?.applyTo(song) ?: song
            }
            missingAlbum -> {
                autoFullMetadataCandidate(song)?.applyTo(song) ?: song
            }
            missingReleaseDate -> {
                autoFullMetadataCandidate(song)?.applyTo(song) ?: song
            }
            missingLyrics -> {
                val lyrics = successfulInBatches(
                    lyricsLoaders(song),
                    score = { it.score }
                )
                if (lyrics == null) song else song.copy(
                    lyrics = lyrics.lyrics,
                    lyricsProvider = lyrics.provider
                )
            }
            missingArtwork -> {
                val artwork = successfulInBatches(
                    artworkLoaders(song),
                    score = { it.score }
                )
                if (artwork == null) song else song.copy(
                    artworkUri = artwork.imageUri,
                    artworkProvider = artwork.provider
                )
            }
            else -> song
        }
    }

    private suspend fun autoFullMetadataCandidate(song: Song): TrackMetadataResult? {
        val loaders = fullMetadataLoaders(song)
        val fastCandidate = bestFullCandidate(loaders.take(FAST_METADATA_SOURCE_COUNT))
        if (fastCandidate != null && fastCandidate.completeness >= FULL_METADATA_THRESHOLD) {
            return fastCandidate
        }
        val fallbackCandidate = bestFullCandidate(loaders.drop(FAST_METADATA_SOURCE_COUNT))
        return listOfNotNull(fastCandidate, fallbackCandidate)
            .maxWithOrNull(compareBy<TrackMetadataResult> { it.completeness }.thenBy { it.score })
    }

    private suspend fun lyricsByProvider(song: Song, provider: MetadataProvider): LyricsResult? {
        return when (provider) {
            MetadataProvider.AUTO -> bestSuccessful(
                *lyricsLoaders(song).toTypedArray()
            )
            MetadataProvider.LRCLIB -> cachedLyrics("lrclib-lyrics", song) { lrcLibScraper.findLyrics(song) }
            MetadataProvider.NETEASE -> cachedLyrics("netease-lyrics", song) { netEaseScraper.findLyrics(song) }
            MetadataProvider.MEGALOBIZ -> cachedLyrics("megalobiz-lyrics", song) { megalobizScraper.findLyrics(song) }
            MetadataProvider.LYRICS_OVH -> cachedLyrics("lyrics-ovh-lyrics", song) { lyricsOvhScraper.findLyrics(song) }
            MetadataProvider.QQ_MUSIC -> cachedLyrics("qq-lyrics", song) { qqMusicScraper.findLyrics(song) }
            MetadataProvider.KUGOU -> cachedLyrics("kugou-lyrics", song) { kuGouScraper.findLyrics(song) }
            MetadataProvider.KUWO -> cachedLyrics("kuwo-lyrics", song) { kuwoScraper.findLyrics(song) }
            else -> null
        }
    }

    private suspend fun artworkByProvider(song: Song, provider: MetadataProvider): ArtworkResult? {
        return when (provider) {
            MetadataProvider.AUTO -> bestSuccessful(
                *artworkLoaders(song).toTypedArray()
            )
            MetadataProvider.MUSICBRAINZ_COVER_ART -> cachedArtwork("cover-art-archive-artwork", song) {
                coverArtArchiveScraper.findArtwork(song)
            }
            MetadataProvider.ITUNES -> cachedArtwork("itunes-artwork", song) { iTunesScraper.findArtwork(song) }
            MetadataProvider.DEEZER -> cachedArtwork("deezer-artwork", song) { deezerScraper.findArtwork(song) }
            MetadataProvider.NETEASE -> cachedArtwork("netease-artwork", song) { netEaseScraper.findFallbackArtwork(song) }
            MetadataProvider.QQ_MUSIC -> cachedArtwork("qq-artwork", song) { qqMusicScraper.findArtwork(song) }
            MetadataProvider.KUGOU -> cachedArtwork("kugou-artwork", song) { kuGouScraper.findArtwork(song) }
            MetadataProvider.KUWO -> cachedArtwork("kuwo-artwork", song) { kuwoScraper.findArtwork(song) }
            else -> null
        }
    }

    data class AllCandidates(
        val tracks: List<TrackMetadataResult>,
        val lyrics: List<LyricsResult>,
        val artworks: List<ArtworkResult>
    )

    suspend fun collectAllCandidates(song: Song): AllCandidates {
        val normalized = musicBrainzScraper.normalize(song)
        val searchSong = normalized.withoutExistingMetadata()
        return coroutineScope {
            val candidateListJobs = listOf(
                async {
                    cachedTrackCandidates("qq-candidates", searchSong) {
                        qqMusicScraper.collectMetadataCandidates(searchSong)
                    }
                },
                async {
                    cachedTrackCandidates("netease-candidates", searchSong) {
                        netEaseScraper.collectMetadataCandidates(searchSong)
                    }
                },
                async {
                    cachedTrackCandidates("itunes-candidates", searchSong) {
                        iTunesScraper.collectMetadataCandidates(searchSong)
                    }
                }
            )
            val candidateJobs = listOf(
                async { cachedTrack("kuwo-candidate", searchSong) { kuwoScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("kugou-candidate", searchSong) { kuGouScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("migu-candidate", searchSong) { miguScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("lrclib-candidate", searchSong) { lrcLibScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("megalobiz-candidate", searchSong) { megalobizScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("lyrics-ovh-candidate", searchSong) { lyricsOvhScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("cover-art-archive-candidate", searchSong) { coverArtArchiveScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("deezer-candidate", searchSong) { deezerScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("genius-candidate", searchSong) { geniusScraper.collectMetadataCandidate(searchSong) } },
                async { cachedTrack("jiosaavn-candidate", searchSong) { jioSaavnScraper.collectMetadataCandidate(searchSong) } }
            )

            val tracks = (candidateListJobs.awaitAll().flatten() + candidateJobs.awaitAll().filterNotNull())
                .filter { it.hasMetadata }
                .sortedByDescending { it.score }

            AllCandidates(
                tracks = tracks,
                lyrics = tracks.mapNotNull { it.lyrics }.sortedByDescending { it.score },
                artworks = tracks.mapNotNull { it.artwork }.sortedByDescending { it.score }
            )
        }
    }

    override suspend fun findArtistArtwork(artistName: String): ArtistArtworkResult? {
        return bestSuccessful(
            { cachedArtistArtwork("qq-artist-artwork", artistName) { qqMusicScraper.findArtistArtwork(artistName) } },
            { cachedArtistArtwork("netease-artist-artwork", artistName) { netEaseScraper.findArtistArtwork(artistName) } }
        )
    }

    private fun fullMetadataLoaders(song: Song): List<suspend () -> TrackMetadataResult?> {
        val cjkFirst = song.prefersCjkSources()
        val cjkLoaders = listOf<suspend () -> TrackMetadataResult?>(
            { cachedTrack("qq-candidate", song) { qqMusicScraper.collectMetadataCandidate(song) } },
            { cachedTrack("netease-candidate", song) { netEaseScraper.collectMetadataCandidate(song) } },
            { cachedTrack("kuwo-candidate", song) { kuwoScraper.collectMetadataCandidate(song) } },
            { cachedTrack("kugou-candidate", song) { kuGouScraper.collectMetadataCandidate(song) } },
            { cachedTrack("migu-candidate", song) { miguScraper.collectMetadataCandidate(song) } }
        )
        val internationalLoaders = listOf<suspend () -> TrackMetadataResult?>(
            { cachedTrack("lrclib-candidate", song) { lrcLibScraper.collectMetadataCandidate(song) } },
            { cachedTrack("itunes-candidate", song) { iTunesScraper.collectMetadataCandidate(song) } },
            { cachedTrack("deezer-candidate", song) { deezerScraper.collectMetadataCandidate(song) } }
        )
        return if (cjkFirst) {
            cjkLoaders + internationalLoaders
        } else {
            internationalLoaders + cjkLoaders
        }
    }

    private fun lyricsLoaders(song: Song): List<suspend () -> LyricsResult?> {
        val cjkLoaders = listOf<suspend () -> LyricsResult?>(
            { cachedLyrics("qq-lyrics", song) { qqMusicScraper.findLyrics(song) } },
            { cachedLyrics("netease-lyrics", song) { netEaseScraper.findLyrics(song) } },
            { cachedLyrics("kuwo-lyrics", song) { kuwoScraper.findLyrics(song) } },
            { cachedLyrics("kugou-lyrics", song) { kuGouScraper.findLyrics(song) } },
            { cachedLyrics("migu-lyrics", song) { miguScraper.findLyrics(song) } }
        )
        val internationalLoaders = listOf<suspend () -> LyricsResult?>(
            { cachedLyrics("lrclib-lyrics", song) { lrcLibScraper.findLyrics(song) } },
            { cachedLyrics("megalobiz-lyrics", song) { megalobizScraper.findLyrics(song) } },
            { cachedLyrics("lyrics-ovh-lyrics", song) { lyricsOvhScraper.findLyrics(song) } }
        )
        return if (song.prefersCjkSources()) cjkLoaders + internationalLoaders else internationalLoaders + cjkLoaders
    }

    private fun artworkLoaders(song: Song): List<suspend () -> ArtworkResult?> {
        val cjkLoaders = listOf<suspend () -> ArtworkResult?>(
            { cachedArtwork("qq-artwork", song) { qqMusicScraper.findArtwork(song) } },
            { cachedArtwork("netease-artwork", song) { netEaseScraper.findFallbackArtwork(song) } },
            { cachedArtwork("kuwo-artwork", song) { kuwoScraper.findArtwork(song) } },
            { cachedArtwork("kugou-artwork", song) { kuGouScraper.findArtwork(song) } },
            { cachedArtwork("migu-artwork", song) { miguScraper.findArtwork(song) } }
        )
        val internationalLoaders = listOf<suspend () -> ArtworkResult?>(
            { cachedArtwork("itunes-artwork", song) { iTunesScraper.findArtwork(song) } },
            { cachedArtwork("deezer-artwork", song) { deezerScraper.findArtwork(song) } },
            { cachedArtwork("cover-art-archive-artwork", song) { coverArtArchiveScraper.findArtwork(song) } }
        )
        return if (song.prefersCjkSources()) cjkLoaders + internationalLoaders else internationalLoaders + cjkLoaders
    }

    private suspend fun bestFullCandidate(loaders: List<suspend () -> TrackMetadataResult?>): TrackMetadataResult? {
        return coroutineScope {
            loaders.map { loader ->
                async { runCatching { loader() }.getOrNull() }
            }
                .awaitAll()
                .filterNotNull()
                .maxWithOrNull(compareBy<TrackMetadataResult> { it.completeness }.thenBy { it.score })
        }
    }

    private suspend fun <T : Any> successfulInBatches(
        loaders: List<suspend () -> T?>,
        score: (T) -> Int
    ): T? {
        for (batch in loaders.chunked(FAST_METADATA_SOURCE_COUNT)) {
            val result = coroutineScope {
                batch.map { loader ->
                    async { runCatching { loader() }.getOrNull() }
                }
                    .awaitAll()
                    .filterNotNull()
                    .maxByOrNull(score)
            }
            if (result != null) return result
        }
        return null
    }

    private suspend fun cachedTrack(
        providerKey: String,
        song: Song,
        loader: suspend () -> TrackMetadataResult?
    ): TrackMetadataResult? = cachedResult(
        cacheKey = song.scrapeCacheKey("track:$providerKey"),
        provider = providerKey,
        parse = ::trackFromPayload,
        encode = { it.toPayload() },
        loader = loader
    )

    private suspend fun cachedTrackCandidates(
        providerKey: String,
        song: Song,
        loader: suspend () -> List<TrackMetadataResult>
    ): List<TrackMetadataResult> = cachedListResult(
        cacheKey = song.scrapeCacheKey("tracks:$providerKey"),
        provider = providerKey,
        parse = ::trackListFromPayload,
        encode = { it.toPayload() },
        loader = loader
    )

    private suspend fun cachedLyrics(
        providerKey: String,
        song: Song,
        loader: suspend () -> LyricsResult?
    ): LyricsResult? = cachedResult(
        cacheKey = song.scrapeCacheKey("lyrics:$providerKey"),
        provider = providerKey,
        parse = ::lyricsFromPayload,
        encode = { it.toPayload() },
        loader = loader
    )

    private suspend fun cachedArtwork(
        providerKey: String,
        song: Song,
        loader: suspend () -> ArtworkResult?
    ): ArtworkResult? = cachedResult(
        cacheKey = song.scrapeCacheKey("artwork:$providerKey"),
        provider = providerKey,
        parse = ::artworkFromPayload,
        encode = { it.toPayload() },
        loader = loader
    )

    private suspend fun cachedArtistArtwork(
        providerKey: String,
        artistName: String,
        loader: suspend () -> ArtistArtworkResult?
    ): ArtistArtworkResult? = cachedResult(
        cacheKey = "scrape:artist-artwork:$providerKey:${normalized(artistName)}",
        provider = providerKey,
        parse = ::artistArtworkFromPayload,
        encode = { it.toPayload() },
        loader = loader
    )

    private suspend fun <T : Any> cachedResult(
        cacheKey: String,
        provider: String,
        parse: (String) -> T?,
        encode: (T) -> String,
        loader: suspend () -> T?
    ): T? {
        val now = System.currentTimeMillis()
        cacheManager.get(cacheKey)?.takeIf { it.isFresh(now) }?.let { cached ->
            if (cached.payload == CACHE_MISS_PAYLOAD) return null
            parse(cached.payload)?.let { return it }
        }
        val result = withTimeoutOrNull(SCRAPER_LOADER_TIMEOUT_MS) {
            runCatching { loader() }.getOrNull()
        }
        cacheManager.put(
            ScrapeCache(
                cacheKey = cacheKey,
                provider = provider,
                payload = result?.let(encode) ?: CACHE_MISS_PAYLOAD,
                updatedAtEpochMs = now
            )
        )
        return result
    }

    private suspend fun cachedListResult(
        cacheKey: String,
        provider: String,
        parse: (String) -> List<TrackMetadataResult>?,
        encode: (List<TrackMetadataResult>) -> String,
        loader: suspend () -> List<TrackMetadataResult>
    ): List<TrackMetadataResult> {
        val now = System.currentTimeMillis()
        cacheManager.get(cacheKey)?.takeIf { it.isFresh(now) }?.let { cached ->
            if (cached.payload == CACHE_MISS_PAYLOAD) return emptyList()
            parse(cached.payload)?.let { return it }
        }
        val result = withTimeoutOrNull(SCRAPER_LOADER_TIMEOUT_MS) {
            runCatching { loader() }.getOrNull()
        }.orEmpty()
        cacheManager.put(
            ScrapeCache(
                cacheKey = cacheKey,
                provider = provider,
                payload = if (result.isEmpty()) CACHE_MISS_PAYLOAD else encode(result),
                updatedAtEpochMs = now
            )
        )
        return result
    }

    private suspend fun <T : Any> bestSuccessful(vararg loaders: suspend () -> T?): T? = coroutineScope {
        loaders.map { loader ->
            async { runCatching { loader() }.getOrNull() }
        }
            .awaitAll()
            .filterNotNull()
            .maxByOrNull { result ->
                when (result) {
                    is LyricsResult -> result.score
                    is ArtworkResult -> result.score
                    is ArtistArtworkResult -> result.score
                    is TrackMetadataResult -> result.score
                    else -> 0
                }
            }
    }

    private suspend fun <T : Any> firstSuccessful(vararg loaders: suspend () -> T?): T? {
        for (loader in loaders) {
            val result = runCatching { loader() }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    private suspend fun <T : Any> allSuccessful(vararg loaders: suspend () -> T?): List<T> = coroutineScope {
        @Suppress("UNCHECKED_CAST")
        loaders.map { loader ->
            async { runCatching { loader() }.getOrNull() }
        }
            .awaitAll()
            .filterNotNull()
            .sortedByDescending { result ->
                when (result) {
                    is LyricsResult -> result.score
                    is ArtworkResult -> result.score
                    is TrackMetadataResult -> result.score
                    else -> 0
                }
            }
    }
}

private val TrackMetadataResult.completeness: Int
    get() = listOfNotNull(lyrics, artwork, albumTitle, releaseYear).size

private fun TrackMetadataResult.applyTo(song: Song): Song {
    return song.copy(
        title = title?.takeIf { it.isNotBlank() } ?: song.title,
        artistName = artistName?.takeIf { it.isNotBlank() } ?: song.artistName,
        albumTitle = albumTitle?.takeIf { it.isNotBlank() } ?: song.albumTitle,
        releaseYear = releaseYear ?: song.releaseYear,
        releaseMonth = releaseMonth ?: song.releaseMonth,
        lyrics = lyrics?.lyrics ?: song.lyrics,
        lyricsProvider = lyrics?.provider ?: song.lyricsProvider,
        artworkUri = artwork?.imageUri ?: song.artworkUri,
        artworkProvider = artwork?.provider ?: song.artworkProvider
    )
}

private fun Song.toCurrentLibraryCandidate(): TrackMetadataResult? {
    val provider = "Current Library"
    val lyricsResult = lyrics?.takeIf { it.isNotBlank() }?.let {
        LyricsResult(
            provider = lyricsProvider ?: provider,
            lyrics = it,
            synced = it.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = 40
        )
    }
    val artworkResult = artworkUri?.takeIf { it.isNotBlank() }?.let {
        ArtworkResult(
            provider = artworkProvider ?: provider,
            imageUri = it,
            score = 40
        )
    }
    val album = albumTitle.takeUnless { it.isUnresolvedAlbumTitle() }
    if (lyricsResult == null && artworkResult == null && album == null && releaseYear == null) return null
    return TrackMetadataResult(
        provider = provider,
        title = title.takeIf { it.isNotBlank() },
        artistName = artistName.takeIf { it.isNotBlank() && it != "Unknown Artist" && it != "Local Folder" },
        albumTitle = album,
        releaseYear = releaseYear,
        releaseMonth = releaseMonth,
        lyrics = lyricsResult,
        artwork = artworkResult,
        score = 40
    )
}

private fun Song.withoutExistingMetadata(): Song {
    return copy(
        lyrics = null,
        lyricsProvider = null,
        artworkUri = null,
        artworkProvider = null,
        releaseYear = null,
        releaseMonth = null
    )
}

private fun ScrapeCache.isFresh(now: Long): Boolean {
    val maxAge = if (payload == CACHE_MISS_PAYLOAD) CACHE_MISS_TTL_MS else CACHE_HIT_TTL_MS
    return updatedAtEpochMs > 0L && now - updatedAtEpochMs <= maxAge
}

private fun Song.scrapeCacheKey(prefix: String): String {
    val artist = artistName.takeUnless { it == "Unknown Artist" || it == "Local Folder" }
    val album = albumTitle.takeUnless { it.isUnresolvedAlbumTitle() }
    val durationSeconds = durationMs.takeIf { it > 0L }?.div(1000L)?.toString().orEmpty()
    return listOf(
        "scrape",
        prefix,
        normalized(title),
        normalized(artist),
        normalized(album),
        durationSeconds
    ).joinToString(":")
}

private fun Song.prefersCjkSources(): Boolean {
    return CJK_TEXT_REGEX.containsMatchIn("$title $artistName $albumTitle")
}

private fun trackFromPayload(payload: String): TrackMetadataResult? {
    return runCatching { JSONObject(payload).toTrackMetadataResult() }.getOrNull()
}

private fun trackListFromPayload(payload: String): List<TrackMetadataResult>? {
    return runCatching {
        JSONArray(payload).objects().mapNotNull { it.toTrackMetadataResult() }.toList()
    }.getOrNull()
}

private fun lyricsFromPayload(payload: String): LyricsResult? {
    return runCatching { JSONObject(payload).toLyricsResult() }.getOrNull()
}

private fun artworkFromPayload(payload: String): ArtworkResult? {
    return runCatching { JSONObject(payload).toArtworkResult() }.getOrNull()
}

private fun artistArtworkFromPayload(payload: String): ArtistArtworkResult? {
    return runCatching { JSONObject(payload).toArtistArtworkResult() }.getOrNull()
}

private fun TrackMetadataResult.toPayload(): String = toJsonObject().toString()

private fun List<TrackMetadataResult>.toPayload(): String {
    val array = JSONArray()
    forEach { array.put(it.toJsonObject()) }
    return array.toString()
}

private fun LyricsResult.toPayload(): String = toJsonObject().toString()

private fun ArtworkResult.toPayload(): String = toJsonObject().toString()

private fun ArtistArtworkResult.toPayload(): String = toJsonObject().toString()

private fun TrackMetadataResult.toJsonObject(): JSONObject {
    return JSONObject()
        .put("provider", provider)
        .put("score", score)
        .putOptional("title", title)
        .putOptional("artistName", artistName)
        .putOptional("albumTitle", albumTitle)
        .putOptional("releaseYear", releaseYear)
        .putOptional("releaseMonth", releaseMonth)
        .putOptional("lyrics", lyrics?.toJsonObject())
        .putOptional("artwork", artwork?.toJsonObject())
}

private fun LyricsResult.toJsonObject(): JSONObject {
    return JSONObject()
        .put("provider", provider)
        .put("lyrics", lyrics)
        .put("synced", synced)
        .put("score", score)
}

private fun ArtworkResult.toJsonObject(): JSONObject {
    return JSONObject()
        .put("provider", provider)
        .put("imageUri", imageUri)
        .put("score", score)
}

private fun ArtistArtworkResult.toJsonObject(): JSONObject {
    return JSONObject()
        .put("provider", provider)
        .put("imageUri", imageUri)
        .put("score", score)
}

private fun JSONObject.toTrackMetadataResult(): TrackMetadataResult? {
    return TrackMetadataResult(
        provider = optionalString("provider") ?: return null,
        title = optionalString("title"),
        artistName = optionalString("artistName"),
        albumTitle = optionalString("albumTitle"),
        releaseYear = optionalInt("releaseYear"),
        releaseMonth = optionalInt("releaseMonth"),
        lyrics = optJSONObject("lyrics")?.toLyricsResult(),
        artwork = optJSONObject("artwork")?.toArtworkResult(),
        score = optInt("score", 0)
    )
}

private fun JSONObject.toLyricsResult(): LyricsResult? {
    return LyricsResult(
        provider = optionalString("provider") ?: return null,
        lyrics = optionalString("lyrics") ?: return null,
        synced = optBoolean("synced", false),
        score = optInt("score", 0)
    )
}

private fun JSONObject.toArtworkResult(): ArtworkResult? {
    return ArtworkResult(
        provider = optionalString("provider") ?: return null,
        imageUri = optionalString("imageUri") ?: return null,
        score = optInt("score", 0)
    )
}

private fun JSONObject.toArtistArtworkResult(): ArtistArtworkResult? {
    return ArtistArtworkResult(
        provider = optionalString("provider") ?: return null,
        imageUri = optionalString("imageUri") ?: return null,
        score = optInt("score", 0)
    )
}

private fun JSONObject.putOptional(key: String, value: String?): JSONObject {
    if (!value.isNullOrBlank()) put(key, value)
    return this
}

private fun JSONObject.putOptional(key: String, value: Int?): JSONObject {
    if (value != null) put(key, value)
    return this
}

private fun JSONObject.putOptional(key: String, value: JSONObject?): JSONObject {
    if (value != null) put(key, value)
    return this
}

private fun JSONObject.optionalString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optionalInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private const val FAST_METADATA_SOURCE_COUNT = 3
private const val FULL_METADATA_THRESHOLD = 3
private const val SCRAPER_LOADER_TIMEOUT_MS = 10_000L
private const val CACHE_HIT_TTL_MS = 14L * 24L * 60L * 60L * 1000L
private const val CACHE_MISS_TTL_MS = 2L * 24L * 60L * 60L * 1000L
private const val CACHE_MISS_PAYLOAD = "__SCRAPE_CACHE_MISS__"
private val CJK_TEXT_REGEX = Regex("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\u3040-\\u30FF\\uAC00-\\uD7AF]")
