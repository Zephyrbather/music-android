package com.musicplayer.app.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.domain.model.isUnresolvedAlbumTitle
import com.musicplayer.app.domain.model.stableMusicId
import com.musicplayer.app.performance.CpuPerformanceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cpuPerformanceManager: CpuPerformanceManager
) {
    suspend fun scan(
        folderUris: List<String> = emptyList(),
        onBatch: suspend (List<Song>) -> Unit = {}
    ): List<Song> = withContext(Dispatchers.IO) {
        cpuPerformanceManager.setBackgroundPriority()
        val mediaPaths = mutableSetOf<String>()
        val (mediaStoreSongs, folderSongs) = coroutineScope {
            val mediaDeferred = async { runCatching { queryMediaStore(mediaPaths) }.getOrDefault(emptyList()) }
            val folderDeferred = async {
                folderUris.flatMap { uri ->
                    runCatching { scanTree(Uri.parse(uri)) }.getOrDefault(emptyList())
                }
            }
            val media = mediaDeferred.await()
            if (media.isNotEmpty()) onBatch(media)
            val folders = folderDeferred.await()
            if (folders.isNotEmpty()) onBatch(folders)
            Pair(media, folders)
        }
        val safPaths = mutableSetOf<String>()
        val dedupedFolders = folderSongs.filter { song ->
            val path = resolveSafPath(song.uri)
            if (path != null) {
                if (path in mediaPaths) return@filter false
                safPaths.add(path)
            }
            true
        }
        val allSongs = mediaStoreSongs + dedupedFolders
        allSongs.also { if (it.isNotEmpty()) onBatch(it) }.ifEmpty { demoLibrary() }
    }

    private fun queryMediaStore(mediaPaths: MutableSet<String>): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"
        val songs = mutableListOf<Song>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val bitrateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val embedded = readEmbeddedMetadata(uri)
                val mediaTitle = cursor.getString(titleColumn).orUnknown("Untitled")
                val mediaArtist = cursor.getString(artistColumn).orUnknown("Unknown Artist")
                val parsed = if (embedded.title == null || embedded.artist == null) parseFilenameArtistTitle(mediaTitle) else null
                val title = embedded.title ?: parsed?.title ?: mediaTitle
                val artist = embedded.artist ?: parsed?.artist ?: mediaArtist
                val album = embedded.album ?: cursor.getString(albumColumn).orUnknown("Unknown Album")
                songs += Song(
                    id = "local-$id",
                    title = title,
                    artistId = stableId("artist", artist),
                    artistName = artist,
                    albumId = stableAlbumId(artist, album),
                    albumTitle = album,
                    durationMs = embedded.durationMs ?: cursor.getLong(durationColumn),
                    trackNumber = cursor.getInt(trackColumn).takeIf { it > 0 },
                    uri = uri.toString(),
                    source = Source.LOCAL,
                    artworkUri = embedded.artworkUri,
                    artworkProvider = embedded.artworkUri?.let { "Embedded" },
                    lyrics = embedded.lyrics,
                    lyricsProvider = embedded.lyrics?.let { "Embedded" },
                    mimeType = embedded.mimeType ?: cursor.getString(mimeColumn),
                    bitrateKbps = embedded.bitrateKbps ?: bitrateColumn
                        .takeIf { it >= 0 }
                        ?.let { cursor.getInt(it) }
                        ?.takeIf { it > 0 }
                        ?.div(1000),
                    releaseYear = embedded.releaseYear,
                    releaseMonth = embedded.releaseMonth,
                    sampleRateHz = embedded.sampleRateHz,
                    dateAddedEpochMs = cursor.getLong(dateAddedColumn) * 1000L
                )
                cursor.getString(dataColumn)?.let { mediaPaths.add(it) }
            }
        }

        return songs
    }

    private fun scanTree(treeUri: Uri): List<Song> {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        return scanDocumentChildren(treeUri, rootDocumentId)
    }

    private fun scanDocumentChildren(treeUri: Uri, parentDocumentId: String): List<Song> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val entries = mutableListOf<DocumentEntry>()

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn).orEmpty()
                if (name.startsWith(".") || name.startsWith("._")) continue
                entries += DocumentEntry(
                    documentId = cursor.getString(idColumn),
                    name = name,
                    mimeType = cursor.getString(mimeColumn),
                    modified = cursor.getLong(modifiedColumn)
                )
            }

            val sidecarsByBase = entries
                .filter { !it.isDirectory && (it.name.isLyricsName() || it.name.isArtworkName()) }
                .groupBy { it.name.substringBeforeLast('.', it.name).lowercase() }

            entries.forEach { entry ->
                if (entry.isDirectory) {
                    songs += scanDocumentChildren(treeUri, entry.documentId)
                } else if (entry.name.isSupportedAudioName()) {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.documentId)
                    val embedded = readEmbeddedMetadata(documentUri)
                    val base = entry.name.substringBeforeLast('.', entry.name)
                    val sidecars = sidecarsByBase[base.lowercase()].orEmpty()
                    val lyricsEntry = sidecars.firstOrNull { it.name.isLyricsName() }
                    val artworkEntry = sidecars.firstOrNull { it.name.isArtworkName() }
                val sidecarLyrics = lyricsEntry?.let {
                    readTextDocument(DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId))
                }
                val lyrics = embedded.lyrics ?: sidecarLyrics
                val sidecarArtworkUri = artworkEntry?.let {
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                }
                    val parsed = if (embedded.title == null || embedded.artist == null) parseFilenameArtistTitle(entry.name) else null
                    val title = embedded.title ?: parsed?.title ?: base
                    val artist = embedded.artist ?: parsed?.artist ?: "Local Folder"
                    val album = embedded.album ?: "待刮削"
                    songs += Song(
                        id = "folder-${documentUri.toString().stableHash()}",
                        title = title,
                        artistId = stableId("artist", artist),
                        artistName = artist,
                        albumId = stableAlbumId(artist, album),
                        albumTitle = album,
                        durationMs = embedded.durationMs ?: 0L,
                        uri = documentUri.toString(),
                        source = Source.LOCAL,
                        artworkUri = embedded.artworkUri ?: sidecarArtworkUri,
                        artworkProvider = when {
                        embedded.artworkUri != null -> "Embedded"
                        sidecarArtworkUri != null -> "Sidecar"
                        else -> null
                    },
                    lyrics = lyrics,
                    lyricsProvider = when {
                        embedded.lyrics != null -> "Embedded"
                        sidecarLyrics != null -> "Sidecar"
                        else -> null
                    },
                    mimeType = embedded.mimeType ?: entry.mimeType,
                    bitrateKbps = embedded.bitrateKbps,
                    sampleRateHz = embedded.sampleRateHz,
                    releaseYear = embedded.releaseYear,
                    releaseMonth = embedded.releaseMonth,
                        dateAddedEpochMs = entry.modified.takeIf { it > 0L }
                            ?: System.currentTimeMillis()
                    )
                }
            }
        }

        return songs
    }

    private fun readEmbeddedMetadata(uri: Uri): EmbeddedMetadata {
        val tagMetadata = readEmbeddedTagMetadata(uri)
        return readMetadataWithRetriever(uri, tagMetadata) { retriever ->
            retriever.setDataSource(context, uri)
        }
    }

    internal suspend fun readRemoteEmbeddedMetadata(
        remoteUri: String,
        requestHeaders: Map<String, String>,
        prefixBytes: ByteArray
    ): EmbeddedMetadata = withContext(Dispatchers.IO) {
        val audioUri = Uri.parse(remoteUri)
        val tagMetadata = parseEmbeddedTagMetadata(audioUri, prefixBytes)
        readMetadataWithRetriever(audioUri, tagMetadata) { retriever ->
            retriever.setDataSource(remoteUri, requestHeaders)
        }
    }

    private fun readMetadataWithRetriever(
        audioUri: Uri,
        tagMetadata: EmbeddedMetadata,
        setDataSource: (MediaMetadataRetriever) -> Unit
    ): EmbeddedMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            setDataSource(retriever)
            val picture = retriever.embeddedPicture
            EmbeddedMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() } ?: tagMetadata.title,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() } ?: tagMetadata.artist,
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf { it.isNotBlank() } ?: tagMetadata.album,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: tagMetadata.durationMs,
                bitrateKbps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()
                    ?.div(1000) ?: tagMetadata.bitrateKbps,
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull() ?: tagMetadata.sampleRateHz,
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    ?.takeIf { it.isNotBlank() } ?: tagMetadata.mimeType,
                artworkUri = picture?.let { saveEmbeddedArtwork(audioUri, it) } ?: tagMetadata.artworkUri,
                lyrics = tagMetadata.lyrics,
                releaseYear = tagMetadata.releaseYear,
                releaseMonth = tagMetadata.releaseMonth
            )
        } catch (_: Throwable) {
            tagMetadata
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readEmbeddedTagMetadata(uri: Uri): EmbeddedMetadata {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use {
                it.readPrefix(maxEmbeddedMetadataBytes)
            } ?: return@runCatching EmbeddedMetadata()
            parseEmbeddedTagMetadata(uri, bytes)
        }.getOrDefault(EmbeddedMetadata())
    }

    private fun parseEmbeddedTagMetadata(uri: Uri, bytes: ByteArray): EmbeddedMetadata {
        return when {
            bytes.startsWithAscii("ID3") -> parseId3Tag(uri, bytes)
            bytes.startsWithAscii("fLaC") -> parseFlacMetadata(uri, bytes)
            else -> EmbeddedMetadata()
        }
    }

    private fun parseId3Tag(uri: Uri, bytes: ByteArray): EmbeddedMetadata {
        if (bytes.size < 10) return EmbeddedMetadata()
        val majorVersion = bytes[3].toInt() and 0xFF
        val tagEnd = minOf(bytes.size, 10 + synchsafeInt(bytes, 6))
        var offset = 10
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var lyrics: String? = null
        var artworkUri: String? = null
        var parsedDate: Pair<Int, Int>? = null

        while (offset < tagEnd) {
            val frame = if (majorVersion == 2) {
                readId3v22Frame(bytes, offset, tagEnd)
            } else {
                readId3v23Frame(bytes, offset, tagEnd, majorVersion)
            } ?: break
            if (frame.id.isBlank() || frame.size <= 0) break

            val data = bytes.copyOfRange(frame.dataStart, frame.dataEnd)
            when (frame.id) {
                "TIT2", "TT2" -> title = title ?: decodeId3Text(data)
                "TPE1", "TP1" -> artist = artist ?: decodeId3Text(data)
                "TALB", "TAL" -> album = album ?: decodeId3Text(data)
                "USLT", "ULT" -> lyrics = lyrics ?: decodeId3Lyrics(data)
                "TXXX", "TXX" -> {
                    val userText = decodeId3UserText(data)
                    if (lyrics == null && userText?.first?.contains("lyric", ignoreCase = true) == true) {
                        lyrics = userText.second
                    }
                }
                "APIC", "PIC" -> artworkUri = artworkUri ?: decodeId3Picture(uri, frame.id, data)
                "TDRC", "TYER", "TDAT" -> {
                    val raw = decodeId3Text(data)
                    if (raw != null) parsedDate = parsedDate ?: parseDateString(raw)
                }
            }
            offset = frame.dataEnd
        }

        return EmbeddedMetadata(
            title = title,
            artist = artist,
            album = album,
            artworkUri = artworkUri,
            lyrics = lyrics,
            releaseYear = parsedDate?.first,
            releaseMonth = parsedDate?.second
        )
    }

    private fun readId3v23Frame(
        bytes: ByteArray,
        offset: Int,
        tagEnd: Int,
        majorVersion: Int
    ): Id3Frame? {
        if (offset + 10 > tagEnd) return null
        val id = bytes.ascii(offset, 4)
        if (id.all { it == '\u0000' }) return null
        val size = if (majorVersion == 4) {
            synchsafeInt(bytes, offset + 4)
        } else {
            bigEndianInt(bytes, offset + 4)
        }
        val dataStart = offset + 10
        val dataEnd = dataStart + size
        if (size <= 0 || dataEnd > tagEnd) return null
        return Id3Frame(id = id, size = size, dataStart = dataStart, dataEnd = dataEnd)
    }

    private fun readId3v22Frame(bytes: ByteArray, offset: Int, tagEnd: Int): Id3Frame? {
        if (offset + 6 > tagEnd) return null
        val id = bytes.ascii(offset, 3)
        if (id.all { it == '\u0000' }) return null
        val size = threeByteInt(bytes, offset + 3)
        val dataStart = offset + 6
        val dataEnd = dataStart + size
        if (size <= 0 || dataEnd > tagEnd) return null
        return Id3Frame(id = id, size = size, dataStart = dataStart, dataEnd = dataEnd)
    }

    private fun decodeId3Text(data: ByteArray): String? {
        if (data.isEmpty()) return null
        return decodeId3EncodedText(data, 1, data.size, data[0].toInt() and 0xFF)
    }

    private fun decodeId3Lyrics(data: ByteArray): String? {
        if (data.size < 5) return null
        val encoding = data[0].toInt() and 0xFF
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1
        val descriptionStart = 4
        val descriptionEnd = findTextTerminator(data, descriptionStart, terminatorSize)
        val textStart = if (descriptionEnd >= 0) descriptionEnd + terminatorSize else descriptionStart
        return decodeId3EncodedText(data, textStart, data.size, encoding)
    }

    private fun decodeId3UserText(data: ByteArray): Pair<String, String>? {
        if (data.size < 2) return null
        val encoding = data[0].toInt() and 0xFF
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1
        val descriptionEnd = findTextTerminator(data, 1, terminatorSize)
        if (descriptionEnd < 0) return null
        val description = decodeId3EncodedText(data, 1, descriptionEnd, encoding) ?: return null
        val value = decodeId3EncodedText(data, descriptionEnd + terminatorSize, data.size, encoding)
            ?: return null
        return description to value
    }

    private fun decodeId3Picture(uri: Uri, frameId: String, data: ByteArray): String? {
        if (data.size < 5) return null
        val encoding = data[0].toInt() and 0xFF
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1
        val imageStart = if (frameId == "PIC") {
            var offset = 1 + 3 + 1
            val descriptionEnd = findTextTerminator(data, offset, terminatorSize)
            offset = if (descriptionEnd >= 0) descriptionEnd + terminatorSize else offset
            offset
        } else {
            val mimeEnd = data.indexOfZero(1)
            if (mimeEnd < 0 || mimeEnd + 2 >= data.size) return null
            var offset = mimeEnd + 2
            val descriptionEnd = findTextTerminator(data, offset, terminatorSize)
            offset = if (descriptionEnd >= 0) descriptionEnd + terminatorSize else offset
            offset
        }
        if (imageStart <= 0 || imageStart >= data.size) return null
        return saveEmbeddedArtwork(uri, data.copyOfRange(imageStart, data.size))
    }

    private fun parseFlacMetadata(uri: Uri, bytes: ByteArray): EmbeddedMetadata {
        var offset = 4
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var lyrics: String? = null
        var artworkUri: String? = null
        var parsedDate: Pair<Int, Int>? = null

        while (offset + 4 <= bytes.size) {
            val header = bytes[offset].toInt() and 0xFF
            val isLast = header and 0x80 != 0
            val type = header and 0x7F
            val length = threeByteInt(bytes, offset + 1)
            val dataStart = offset + 4
            val dataEnd = dataStart + length
            if (length < 0 || dataEnd > bytes.size) break

            when (type) {
                4 -> {
                    val comments = parseVorbisComments(bytes.copyOfRange(dataStart, dataEnd))
                    title = title ?: comments["TITLE"]
                    artist = artist ?: comments["ARTIST"]
                    album = album ?: comments["ALBUM"]
                    lyrics = lyrics ?: comments["LYRICS"]
                        ?: comments["UNSYNCEDLYRICS"]
                        ?: comments["UNSYNCED LYRICS"]
                    val dateStr = comments["DATE"] ?: comments["YEAR"]
                    if (dateStr != null) parsedDate = parsedDate ?: parseDateString(dateStr)
                }
                6 -> artworkUri = artworkUri ?: parseFlacPicture(uri, bytes.copyOfRange(dataStart, dataEnd))
            }

            offset = dataEnd
            if (isLast) break
        }

        return EmbeddedMetadata(
            title = title,
            artist = artist,
            album = album,
            artworkUri = artworkUri,
            lyrics = lyrics,
            releaseYear = parsedDate?.first,
            releaseMonth = parsedDate?.second
        )
    }

    private fun parseVorbisComments(data: ByteArray): Map<String, String> {
        var offset = 0
        val vendorLength = littleEndianInt(data, offset)
        offset += 4 + vendorLength
        if (offset + 4 > data.size) return emptyMap()
        val count = littleEndianInt(data, offset)
        offset += 4
        val comments = mutableMapOf<String, String>()
        repeat(count.coerceAtLeast(0)) {
            if (offset + 4 > data.size) return comments
            val length = littleEndianInt(data, offset)
            offset += 4
            if (length < 0 || offset + length > data.size) return comments
            val comment = data.copyOfRange(offset, offset + length).toString(Charsets.UTF_8)
            offset += length
            val key = comment.substringBefore('=', "").uppercase()
            val value = comment.substringAfter('=', "").trim()
            if (key.isNotBlank() && value.isNotBlank()) comments[key] = value
        }
        return comments
    }

    private fun parseFlacPicture(uri: Uri, data: ByteArray): String? {
        var offset = 0
        if (offset + 8 > data.size) return null
        offset += 4
        val mimeLength = bigEndianInt(data, offset)
        offset += 4 + mimeLength
        if (offset + 4 > data.size) return null
        val descriptionLength = bigEndianInt(data, offset)
        offset += 4 + descriptionLength
        if (offset + 20 > data.size) return null
        offset += 16
        val pictureLength = bigEndianInt(data, offset)
        offset += 4
        if (pictureLength <= 0 || offset + pictureLength > data.size) return null
        return saveEmbeddedArtwork(uri, data.copyOfRange(offset, offset + pictureLength))
    }

    private fun saveEmbeddedArtwork(audioUri: Uri, bytes: ByteArray): String? {
        return runCatching {
            val directory = File(context.cacheDir, "embedded-artwork").apply { mkdirs() }
            val file = File(directory, "${audioUri.toString().stableHash()}.jpg")
            if (!file.exists() || file.length() != bytes.size.toLong()) {
                file.writeBytes(bytes)
            }
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    private fun readTextDocument(uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun demoLibrary(): List<Song> {
        val now = System.currentTimeMillis()
        return listOf(
            Song(
                id = "demo-aurora",
                title = "Aurora Practice",
                artistId = "artist-demo-club",
                artistName = "Bedroom Club",
                albumId = "album-late-night",
                albumTitle = "Late Night Drafts",
                durationMs = 218_000,
                trackNumber = 1,
                uri = "demo://aurora-practice",
                dateAddedEpochMs = now
            ),
            Song(
                id = "demo-paper",
                title = "Paper Sleeve",
                artistId = "artist-demo-club",
                artistName = "Bedroom Club",
                albumId = "album-late-night",
                albumTitle = "Late Night Drafts",
                durationMs = 191_000,
                trackNumber = 2,
                uri = "demo://paper-sleeve",
                dateAddedEpochMs = now - 86_400_000L
            ),
            Song(
                id = "demo-train",
                title = "Train Window",
                artistId = "artist-clear-line",
                artistName = "Clear Line",
                albumId = "album-city-maps",
                albumTitle = "City Maps",
                durationMs = 244_000,
                trackNumber = 4,
                uri = "demo://train-window",
                dateAddedEpochMs = now - 172_800_000L
            )
        )
    }

    private fun resolveSafPath(uri: String): String? {
        return runCatching {
            val docUri = android.net.Uri.parse(uri)
            val docId = DocumentsContract.getDocumentId(docUri)
            val colon = docId.indexOf(':')
            if (colon < 0) return null
            val volume = docId.substring(0, colon)
            val path = docId.substring(colon + 1)
            if (volume == "primary") {
                android.os.Environment.getExternalStorageDirectory().absolutePath + "/" + path
            } else {
                "/storage/$volume/$path"
            }
        }.getOrNull()
    }
}

internal data class EmbeddedMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val bitrateKbps: Int? = null,
    val mimeType: String? = null,
    val artworkUri: String? = null,
    val lyrics: String? = null,
    val releaseYear: Int? = null,
    val releaseMonth: Int? = null,
    val sampleRateHz: Int? = null
)

private data class DocumentEntry(
    val documentId: String,
    val name: String,
    val mimeType: String?,
    val modified: Long
) {
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}

private data class Id3Frame(
    val id: String,
    val size: Int,
    val dataStart: Int,
    val dataEnd: Int
)

private fun java.io.InputStream.readPrefix(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var remaining = maxBytes
    while (remaining > 0) {
        val read = read(buffer, 0, minOf(buffer.size, remaining))
        if (read < 0) break
        output.write(buffer, 0, read)
        remaining -= read
    }
    return output.toByteArray()
}

private fun ByteArray.startsWithAscii(value: String): Boolean {
    if (size < value.length) return false
    return value.indices.all { index -> this[index].toInt().toChar() == value[index] }
}

private fun ByteArray.ascii(offset: Int, length: Int): String {
    if (offset < 0 || offset + length > size) return ""
    return copyOfRange(offset, offset + length).toString(Charsets.ISO_8859_1)
}

private fun ByteArray.indexOfZero(start: Int): Int {
    for (index in start until size) {
        if (this[index].toInt() == 0) return index
    }
    return -1
}

private fun decodeId3EncodedText(data: ByteArray, start: Int, end: Int, encoding: Int): String? {
    if (start >= end || start < 0 || end > data.size) return null
    val bytes = data.copyOfRange(start, end)
    val text = when (encoding) {
        0 -> bytes.toString(Charsets.ISO_8859_1)
        1 -> bytes.toString(Charsets.UTF_16)
        2 -> bytes.toString(Charsets.UTF_16BE)
        3 -> bytes.toString(Charsets.UTF_8)
        else -> bytes.toString(Charsets.UTF_8)
    }
    return text.replace("\u0000", "").trim().takeIf { it.isNotBlank() }
}

private fun findTextTerminator(data: ByteArray, start: Int, terminatorSize: Int): Int {
    var index = start
    while (index + terminatorSize <= data.size) {
        val terminated = (0 until terminatorSize).all { offset ->
            data[index + offset].toInt() == 0
        }
        if (terminated) return index
        index += terminatorSize
    }
    return -1
}

private fun synchsafeInt(data: ByteArray, offset: Int): Int {
    if (offset + 4 > data.size) return 0
    return ((data[offset].toInt() and 0x7F) shl 21) or
        ((data[offset + 1].toInt() and 0x7F) shl 14) or
        ((data[offset + 2].toInt() and 0x7F) shl 7) or
        (data[offset + 3].toInt() and 0x7F)
}

private fun bigEndianInt(data: ByteArray, offset: Int): Int {
    if (offset + 4 > data.size) return 0
    return ((data[offset].toInt() and 0xFF) shl 24) or
        ((data[offset + 1].toInt() and 0xFF) shl 16) or
        ((data[offset + 2].toInt() and 0xFF) shl 8) or
        (data[offset + 3].toInt() and 0xFF)
}

private fun littleEndianInt(data: ByteArray, offset: Int): Int {
    if (offset + 4 > data.size) return 0
    return (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)
}

private fun threeByteInt(data: ByteArray, offset: Int): Int {
    if (offset + 3 > data.size) return 0
    return ((data[offset].toInt() and 0xFF) shl 16) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        (data[offset + 2].toInt() and 0xFF)
}

private fun String?.orUnknown(fallback: String): String {
    return this?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback
}

private fun stableId(prefix: String, value: String): String {
    return stableMusicId(prefix, value)
}

private fun stableAlbumId(artist: String, album: String): String? {
    if (album.isUnresolvedAlbumTitle()) return null
    return stableId("album", "$artist-$album")
}

private fun String.isSupportedAudioName(): Boolean {
    val lower = lowercase()
    return supportedAudioExtensions.any { lower.endsWith(".$it") }
}

private fun String.isLyricsName(): Boolean {
    val lower = lowercase()
    return lower.endsWith(".lrc") || lower.endsWith(".txt")
}

private fun String.isArtworkName(): Boolean {
    val lower = lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
        lower.endsWith(".png") || lower.endsWith(".webp")
}

private fun String.stableHash(): String {
    return Integer.toUnsignedString(hashCode(), 16)
}

private data class ParsedFilename(val title: String, val artist: String?)

private fun parseDateString(raw: String): Pair<Int, Int>? {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return null
    val yearPart = cleaned.substringBefore('-').substringBefore('/').substringBefore('T')
    val year = yearPart.take(4).toIntOrNull()?.takeIf { it in 1900..2100 } ?: return null
    val rest = cleaned.removePrefix(yearPart).removePrefix("-").removePrefix("/")
    val month = rest.take(2).toIntOrNull()?.takeIf { it in 1..12 }
    return year to (month ?: 0)
}

private fun parseFilenameArtistTitle(filename: String): ParsedFilename {
    val base = filename.substringBeforeLast('.', filename).trim()
    if (base.isBlank()) return ParsedFilename(filename, null)
    val separators = listOf(" - ", " – ", "-", "－", " － ")
    for (sep in separators) {
        val idx = base.indexOf(sep)
        if (idx > 0 && idx + sep.length < base.length) {
            val left = base.substring(0, idx).trim()
            val right = base.substring(idx + sep.length).trim()
            if (left.isNotBlank() && right.isNotBlank()) {
                val isFullWidthDash = sep.contains("－")
                return if (isFullWidthDash) {
                    ParsedFilename(title = left, artist = right)
                } else {
                    ParsedFilename(title = right, artist = left)
                }
            }
        }
    }
    return ParsedFilename(title = base, artist = null)
}

private val supportedAudioExtensions = setOf(
    "mp3",
    "flac",
    "m4a",
    "aac",
    "ogg",
    "opus",
    "wav",
    "dsf",
    "dff",
    "aiff",
    "aif"
)

private const val maxEmbeddedMetadataBytes = 8 * 1024 * 1024
