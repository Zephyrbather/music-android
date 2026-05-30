package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.isUnresolvedAlbumTitle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
    private val miguScraper: MiguScraper
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
                val lyrics = firstSuccessful(
                    { qqMusicScraper.findLyrics(song) },
                    { netEaseScraper.findLyrics(song) },
                    { kuwoScraper.findLyrics(song) },
                    { kuGouScraper.findLyrics(song) },
                    { miguScraper.findLyrics(song) },
                    { lrcLibScraper.findLyrics(song) },
                    { megalobizScraper.findLyrics(song) },
                    { lyricsOvhScraper.findLyrics(song) }
                )
                if (lyrics == null) song else song.copy(
                    lyrics = lyrics.lyrics,
                    lyricsProvider = lyrics.provider
                )
            }
            missingArtwork -> {
                val artwork = firstSuccessful(
                    { qqMusicScraper.findArtwork(song) },
                    { netEaseScraper.findFallbackArtwork(song) },
                    { kuwoScraper.findArtwork(song) },
                    { kuGouScraper.findArtwork(song) },
                    { miguScraper.findArtwork(song) },
                    { iTunesScraper.findArtwork(song) },
                    { deezerScraper.findArtwork(song) },
                    { coverArtArchiveScraper.findArtwork(song) }
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
        var bestPartial: TrackMetadataResult? = null
        val loaders = listOf<suspend () -> TrackMetadataResult?>(
            { qqMusicScraper.collectMetadataCandidate(song) },
            { netEaseScraper.collectMetadataCandidate(song) },
            { kuwoScraper.collectMetadataCandidate(song) },
            { kuGouScraper.collectMetadataCandidate(song) },
            { miguScraper.collectMetadataCandidate(song) },
            { lrcLibScraper.collectMetadataCandidate(song) },
            { iTunesScraper.collectMetadataCandidate(song) },
            { deezerScraper.collectMetadataCandidate(song) }
        )
        for (loader in loaders) {
            val candidate = runCatching { loader() }.getOrNull() ?: continue
            if (candidate.lyrics != null && candidate.artwork != null) return candidate
            if (candidate.completeness > (bestPartial?.completeness ?: 0)) {
                bestPartial = candidate
            }
        }
        return bestPartial
    }

    private suspend fun lyricsByProvider(song: Song, provider: MetadataProvider): LyricsResult? {
        return when (provider) {
            MetadataProvider.AUTO -> bestSuccessful(
                { lrcLibScraper.findLyrics(song) },
                { qqMusicScraper.findLyrics(song) },
                { netEaseScraper.findLyrics(song) },
                { kuwoScraper.findLyrics(song) },
                { kuGouScraper.findLyrics(song) },
                { miguScraper.findLyrics(song) },
                { megalobizScraper.findLyrics(song) },
                { lyricsOvhScraper.findLyrics(song) }
            )
            MetadataProvider.LRCLIB -> lrcLibScraper.findLyrics(song)
            MetadataProvider.NETEASE -> netEaseScraper.findLyrics(song)
            MetadataProvider.MEGALOBIZ -> megalobizScraper.findLyrics(song)
            MetadataProvider.LYRICS_OVH -> lyricsOvhScraper.findLyrics(song)
            MetadataProvider.QQ_MUSIC -> qqMusicScraper.findLyrics(song)
            MetadataProvider.KUGOU -> kuGouScraper.findLyrics(song)
            MetadataProvider.KUWO -> kuwoScraper.findLyrics(song)
            else -> null
        }
    }

    private suspend fun artworkByProvider(song: Song, provider: MetadataProvider): ArtworkResult? {
        return when (provider) {
            MetadataProvider.AUTO -> bestSuccessful(
                { qqMusicScraper.findArtwork(song) },
                { netEaseScraper.findFallbackArtwork(song) },
                { kuwoScraper.findArtwork(song) },
                { kuGouScraper.findArtwork(song) },
                { miguScraper.findArtwork(song) },
                { coverArtArchiveScraper.findArtwork(song) },
                { iTunesScraper.findArtwork(song) },
                { deezerScraper.findArtwork(song) }
            )
            MetadataProvider.MUSICBRAINZ_COVER_ART -> coverArtArchiveScraper.findArtwork(song)
            MetadataProvider.ITUNES -> iTunesScraper.findArtwork(song)
            MetadataProvider.DEEZER -> deezerScraper.findArtwork(song)
            MetadataProvider.NETEASE -> netEaseScraper.findFallbackArtwork(song)
            MetadataProvider.QQ_MUSIC -> qqMusicScraper.findArtwork(song)
            MetadataProvider.KUGOU -> kuGouScraper.findArtwork(song)
            MetadataProvider.KUWO -> kuwoScraper.findArtwork(song)
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
            val chineseResultsJob = async {
                qqMusicScraper.collectMetadataCandidates(searchSong) +
                    netEaseScraper.collectMetadataCandidates(searchSong) +
                    listOfNotNull(
                        kuwoScraper.collectMetadataCandidate(searchSong),
                        kuGouScraper.collectMetadataCandidate(searchSong),
                        miguScraper.collectMetadataCandidate(searchSong)
                    )
            }
            val intlLyricsJob = async {
                allSuccessful(
                    { lrcLibScraper.collectMetadataCandidate(searchSong) },
                    { megalobizScraper.collectMetadataCandidate(searchSong) },
                    { lyricsOvhScraper.collectMetadataCandidate(searchSong) }
                )
            }
            val intlArtworkJob = async {
                iTunesScraper.collectMetadataCandidates(searchSong) + allSuccessful(
                    { coverArtArchiveScraper.collectMetadataCandidate(searchSong) },
                    { deezerScraper.collectMetadataCandidate(searchSong) },
                    { geniusScraper.collectMetadataCandidate(searchSong) },
                    { jioSaavnScraper.collectMetadataCandidate(searchSong) }
                )
            }

            val tracks = (
                chineseResultsJob.await() +
                intlLyricsJob.await() +
                intlArtworkJob.await())
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
            { qqMusicScraper.findArtistArtwork(artistName) },
            { netEaseScraper.findArtistArtwork(artistName) }
        )
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
