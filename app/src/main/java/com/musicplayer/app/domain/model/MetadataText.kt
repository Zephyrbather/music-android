package com.musicplayer.app.domain.model

private val UUID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val ZERO_WIDTH_REGEX = Regex("[\\u200B-\\u200D\\u2060\\uFEFF]")
private val CONTROL_REGEX = Regex("[\\u0000-\\u001F\\u007F]")
private val HTML_ENTITY_REGEX = Regex("&(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);")
private val MOJIBAKE_MARKERS = listOf(
    "锟斤拷", "烫烫烫", "\uFFFD", "Ã", "Â", "â€", "ï¿½", "ä¸", "å¤", "æ–", "é‡", "çš"
)

fun String.looksLikeUuid(): Boolean = trim().matches(UUID_REGEX)

fun String.hasInvalidMetadataText(): Boolean {
    if (isBlank()) return true
    if (ZERO_WIDTH_REGEX.containsMatchIn(this)) return true
    if (CONTROL_REGEX.containsMatchIn(this)) return true
    if (HTML_ENTITY_REGEX.containsMatchIn(this)) return true
    return MOJIBAKE_MARKERS.any { contains(it) }
}

fun String.cleanMetadataText(fallback: String): String {
    val cleaned = this
        .replace(ZERO_WIDTH_REGEX, "")
        .replace(CONTROL_REGEX, " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
    val withoutBroken = cleaned.replace("\uFFFD", "").replace(Regex("\\s+"), " ").trim()
    return withoutBroken.ifBlank { fallback }
}

fun String.normalizedForDuplicateMatch(): String {
    return lowercase()
        .replace(Regex("[\\p{Punct}\\s·・，。！？、'\"（）()\\[\\]【】《》—–-]"), "")
        .trim()
}
