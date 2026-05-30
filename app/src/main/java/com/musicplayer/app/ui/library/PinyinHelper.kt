package com.musicplayer.app.ui.library

object PinyinHelper {
    private val transliterator by lazy {
        runCatching {
            android.icu.text.Transliterator.getInstance("Han-Latin")
        }.getOrNull()
    }

    fun toPinyinInitial(c: Char): String? {
        if (c in '0'..'9') return null
        if (c in 'a'..'z' || c in 'A'..'Z') return c.uppercaseChar().toString()
        val tl = transliterator ?: return "#"
        val latin = runCatching { tl.transliterate(c.toString()) }.getOrNull() ?: return "#"
        if (latin == c.toString()) return "#"
        val first = latin.firstOrNull { it in 'a'..'z' || it in 'A'..'Z' } ?: return "#"
        return first.uppercaseChar().toString()
    }

    fun comparePinyin(a: String, b: String): Int {
        val collator = java.text.Collator.getInstance(java.util.Locale.CHINESE)
        collator.strength = java.text.Collator.PRIMARY
        return collator.compare(a, b)
    }
}
