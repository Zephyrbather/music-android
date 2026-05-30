package com.musicplayer.app.data.tag

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song

@Singleton
class AudioTagWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun writeIfSupported(
        song: Song,
        mode: ScrapeSearchMode,
        renameLocalFile: Boolean
    ): Song = withContext(Dispatchers.IO) {
        val destination = song.uri.toWritableDestination() ?: return@withContext song
        try {
            runCatching {
                val taggedFile = AudioFileIO.read(destination.workingFile)
                val tag = taggedFile.tagOrCreateAndSetDefault
                if (mode == ScrapeSearchMode.ALL) {
                    song.title.takeIf { it.isNotBlank() }?.let { tag.setField(FieldKey.TITLE, it) }
                    song.artistName.takeIf { it.isNotBlank() }?.let { tag.setField(FieldKey.ARTIST, it) }
                    song.albumTitle.takeIf { it.isNotBlank() }?.let { tag.setField(FieldKey.ALBUM, it) }
                    song.releaseYear?.takeIf { it > 0 }?.let { tag.setField(FieldKey.YEAR, it.toString()) }
                }
                if (mode != ScrapeSearchMode.ARTWORK) {
                    song.lyrics?.takeIf { it.isNotBlank() }?.let { tag.setField(FieldKey.LYRICS, it) }
                }

                var tempArtwork: File? = null
                try {
                    if (mode != ScrapeSearchMode.LYRICS) {
                        tempArtwork = song.artworkUri?.takeIf { it.isNotBlank() }?.toArtworkTempFile()
                        if (tempArtwork != null) {
                            tag.deleteArtworkField()
                            tag.setField(ArtworkFactory.createArtworkFromFile(tempArtwork))
                        }
                    }
                    taggedFile.commit()
                } finally {
                    tempArtwork?.delete()
                }
                if (!destination.commit()) return@runCatching song
                val finalFile = if (
                    renameLocalFile &&
                    mode == ScrapeSearchMode.ALL &&
                    destination.originalFile != null
                ) {
                    destination.originalFile.renameToScrapedName(song)
                } else {
                    destination.originalFile
                }
                if (finalFile == null || finalFile == destination.originalFile) {
                    song
                } else {
                    song.copy(uri = Uri.fromFile(finalFile).toString())
                }
            }.getOrDefault(song)
        } finally {
            destination.cleanup()
        }
    }

    private fun String.toWritableDestination(): WritableDestination? {
        val uri = Uri.parse(this)
        return when (uri.scheme) {
            "file" -> runCatching { File(requireNotNull(uri.path)) }.getOrNull()
                ?.takeIf { it.isFile && it.canRead() && it.canWrite() }
                ?.takeIf { it.extension.lowercase() in supportedExtensions }
                ?.let { file ->
                    WritableDestination(
                        workingFile = file,
                        originalFile = file,
                        commit = { true },
                        cleanup = {}
                    )
                }
            "content" -> uri.toContentDestination()
            else -> null
        }
    }

    private fun Uri.toContentDestination(): WritableDestination? {
        val extension = contentExtension() ?: return null
        if (extension !in supportedExtensions) return null
        val temporary = File.createTempFile("tag-audio-", ".$extension", context.cacheDir)
        val copied = runCatching {
            context.contentResolver.openInputStream(this)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
        }.getOrDefault(false)
        if (!copied) {
            temporary.delete()
            return null
        }
        return WritableDestination(
            workingFile = temporary,
            originalFile = null,
            commit = {
                runCatching {
                    context.contentResolver.openOutputStream(this, "rwt")?.use { output ->
                        temporary.inputStream().use { input -> input.copyTo(output) }
                        true
                    } ?: false
                }.getOrDefault(false)
            },
            cleanup = { temporary.delete() }
        )
    }

    private fun Uri.contentExtension(): String? {
        val displayName = runCatching {
            context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
        return displayName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: context.contentResolver.getType(this)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.lowercase()
    }

    private fun String.toArtworkTempFile(): File? {
        val uri = Uri.parse(this)
        val bytes = when (uri.scheme) {
            "http", "https" -> {
                val request = Request.Builder().url(this).header("User-Agent", "MusicPlayer Android/0.1").build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.bytes()
                }
            }
            "file", "content" -> {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            else -> null
        } ?: return null
        if (bytes.isEmpty()) return null
        return File.createTempFile("tag-artwork-", ".img", context.cacheDir).apply {
            writeBytes(bytes)
        }
    }

    private fun File.renameToScrapedName(song: Song): File {
        val parent = parentFile ?: return this
        val extension = extension.takeIf { it.isNotBlank() } ?: return this
        val title = song.title.toSafeFilePart().ifBlank { nameWithoutExtension }
        val artist = song.artistName.toSafeFilePart().ifBlank { "Unknown Artist" }
        val target = uniqueTargetFile(parent, "$title－$artist", extension, this)
        if (target.canonicalPath == canonicalPath) return this
        return if (renameTo(target)) target else this
    }

    private fun uniqueTargetFile(
        parent: File,
        baseName: String,
        extension: String,
        original: File
    ): File {
        val first = File(parent, "$baseName.$extension")
        if (!first.exists() || first.canonicalPath == original.canonicalPath) return first
        for (index in 1..999) {
            val candidate = File(parent, "$baseName ($index).$extension")
            if (!candidate.exists() || candidate.canonicalPath == original.canonicalPath) return candidate
        }
        return original
    }

    private fun String.toSafeFilePart(): String {
        return trim()
            .replace(Regex("""[\u0000/\\:*?"<>|]"""), " ")
            .replace(Regex("\\s+"), " ")
            .take(96)
            .trim()
    }

    private companion object {
        val supportedExtensions = setOf("mp3", "flac", "m4a", "mp4", "ogg")
    }

    private data class WritableDestination(
        val workingFile: File,
        val originalFile: File?,
        val commit: () -> Boolean,
        val cleanup: () -> Unit
    )
}
