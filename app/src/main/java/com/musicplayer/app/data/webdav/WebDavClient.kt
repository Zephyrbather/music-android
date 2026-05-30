package com.musicplayer.app.data.webdav

import com.musicplayer.app.domain.model.WebDavServer
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class WebDavTrack(
    val uri: String,
    val name: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null
)

data class WebDavItem(
    val uri: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null
)

@Singleton
class WebDavClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun listAudio(server: WebDavServer): List<WebDavTrack> = withContext(Dispatchers.IO) {
        val request = buildPropfindRequest(server.baseUrl.trimEnd('/'), server, "infinity")
        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) return@withContext emptyList()
            val body = it.body?.string().orEmpty()
            parsePropfind(server.baseUrl.trimEnd('/'), body)
        }
    }

    suspend fun listDirectory(server: WebDavServer, path: String): List<WebDavItem> = withContext(Dispatchers.IO) {
        val base = server.baseUrl.trimEnd('/')
        val url = if (path.isEmpty() || path == "/") base else "$base/${path.trimStart('/')}"
        val request = buildPropfindRequest(url, server, "1")
        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) return@withContext emptyList()
            val body = it.body?.string().orEmpty()
            parseDirectory(base, url, body)
        }
    }

    suspend fun listAudioInFolder(server: WebDavServer, folderPath: String): List<WebDavTrack> = withContext(Dispatchers.IO) {
        val base = server.baseUrl.trimEnd('/')
        val startPath = folderPath.trimStart('/').trimEnd('/')
        val allTracks = mutableListOf<WebDavTrack>()
        val visited = mutableSetOf<String>()

        suspend fun walk(currentPath: String) {
            val dirUrl = if (currentPath.isEmpty() || currentPath == "/") base else "$base/$currentPath"
            if (!visited.add(dirUrl)) return
            Log.d("WebDAV", "listAudioInFolder: walking $dirUrl")

            val items = listDirectory(server, currentPath.ifEmpty { "/" })
            for (item in items) {
                if (item.isDirectory) {
                    val subPath = item.uri.removePrefix("$base/").trimEnd('/')
                    if (subPath.isNotEmpty() && subPath != currentPath) {
                        walk(subPath)
                    }
                } else if (item.name.endsWithAudioExtension() || item.uri.endsWithAudioExtension()) {
                    allTracks.add(WebDavTrack(
                        uri = item.uri,
                        name = item.name,
                        sizeBytes = item.sizeBytes
                    ))
                }
            }
        }

        walk(startPath)
        Log.d("WebDAV", "listAudioInFolder: found ${allTracks.size} tracks via recursive walk")
        allTracks
    }

    suspend fun downloadFile(url: String, server: WebDavServer, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(server.username, server.password))
                .get()
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return@withContext false
                outputFile.parentFile?.mkdirs()
                it.body?.byteStream()?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readPrefix(
        url: String,
        server: WebDavServer,
        maxBytes: Int
    ): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(server.username, server.password))
                .header("Range", "bytes=0-${maxBytes - 1}")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.byteStream()?.use { stream -> stream.readPrefix(maxBytes) }
            }
        }.getOrNull()
    }

    private fun buildPropfindRequest(url: String, server: WebDavServer, depth: String): Request {
        return Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(server.username, server.password))
            .header("Depth", depth)
            .method("PROPFIND", ByteArray(0).toRequestBody(null))
            .build()
    }

    private fun parseDirectory(baseUrl: String, requestUrl: String, xml: String): List<WebDavItem> {
        val entries = Regex("<[^:>]*:?response>(.*?)</[^:>]*:?response>", RegexOption.IGNORE_CASE)
            .findAll(xml)
            .map { it.groupValues[1] }
            .toList()

        return entries.mapNotNull { entry ->
            val href = Regex("<[^:>]*:?href>(.*?)</[^:>]*:?href>", RegexOption.IGNORE_CASE)
                .find(entry)?.groupValues?.get(1)?.trim() ?: return@mapNotNull null
            val decodedHref = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
            val fullUri = if (decodedHref.startsWith("http")) decodedHref else "$baseUrl/${decodedHref.trimStart('/')}"

            if (fullUri.trimEnd('/') == requestUrl.trimEnd('/')) return@mapNotNull null

            val isCollection = entry.contains("<[^:>]*:?collection/>".toRegex(RegexOption.IGNORE_CASE)) ||
                entry.contains("<[^:>]*:?resourcetype>".toRegex(RegexOption.IGNORE_CASE)) && entry.contains("<[^:>]*:?collection".toRegex(RegexOption.IGNORE_CASE))

            val displayName = Regex("<[^:>]*:?displayname>(.*?)</[^:>]*:?displayname>", RegexOption.IGNORE_CASE)
                .find(entry)?.groupValues?.get(1)?.trim()
                ?: decodedHref.split('/').filter { it.isNotBlank() }.lastOrNull()
                ?: decodedHref

            val contentLength = Regex("<[^:>]*:?getcontentlength>(.*?)</[^:>]*:?getcontentlength>", RegexOption.IGNORE_CASE)
                .find(entry)?.groupValues?.get(1)?.trim()?.toLongOrNull()

            WebDavItem(
                uri = fullUri,
                name = displayName.ifBlank { decodedHref.substringAfterLast('/').trimEnd('/') },
                isDirectory = isCollection || decodedHref.endsWith("/"),
                sizeBytes = contentLength
            )
        }
        .sortedWith(compareByDescending<WebDavItem> { it.isDirectory }.thenByDescending { it.sizeBytes ?: 0L }.thenBy { it.name })
    }

    private fun parsePropfind(baseUrl: String, xml: String): List<WebDavTrack> {
        val allHrefs = Regex("<[^:>]*:?href>(.*?)</[^:>]*:?href>", RegexOption.IGNORE_CASE)
            .findAll(xml)
            .map { it.groupValues[1].trim() }
            .toList()
        Log.d("WebDAV", "parsePropfind: total hrefs=${allHrefs.size}, baseUrl=$baseUrl")
        val hrefs = allHrefs
            .filter { it.endsWithAudioExtension() }
            .distinct()
        Log.d("WebDAV", "parsePropfind: audio hrefs=${hrefs.size}, list=${hrefs.take(5)}")

        return hrefs.map { href ->
            val decodedName = runCatching {
                URLDecoder.decode(href.substringAfterLast('/'), "UTF-8")
            }.getOrDefault(href.substringAfterLast('/'))
            WebDavTrack(
                uri = if (href.startsWith("http")) href else "$baseUrl/${href.trimStart('/')}",
                name = decodedName.substringBeforeLast('.', decodedName),
                mimeType = href.toAudioMimeType()
            )
        }.toList()
    }
}

private fun InputStream.readPrefix(maxBytes: Int): ByteArray {
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

private fun String.endsWithAudioExtension(): Boolean {
    val lower = lowercase()
    return listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".dsf", ".dff")
        .any(lower::endsWith)
}

private fun String.toAudioMimeType(): String? {
    return when {
        endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
        endsWith(".flac", ignoreCase = true) -> "audio/flac"
        endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
        endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
        endsWith(".wav", ignoreCase = true) -> "audio/wav"
        endsWith(".dsf", ignoreCase = true) -> "audio/x-dsf"
        endsWith(".dff", ignoreCase = true) -> "audio/x-dff"
        else -> null
    }
}
