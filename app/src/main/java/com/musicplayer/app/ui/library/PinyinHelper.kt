package com.musicplayer.app.ui.library

import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object PinyinHelper {
    private const val NO_INITIAL = ""
    private val initialCache = ConcurrentHashMap<Char, String>()
    private val transliterator by lazy {
        runCatching {
            android.icu.text.Transliterator.getInstance("Han-Latin")
        }.getOrNull()
    }
    private val collator = ThreadLocal.withInitial {
        Collator.getInstance(Locale.CHINESE).apply {
            strength = Collator.PRIMARY
        }
    }

    fun toPinyinInitial(c: Char): String? {
        if (c in '0'..'9') return null
        return initialCache.getOrPut(c) {
            computePinyinInitial(c) ?: NO_INITIAL
        }.takeIf { it.isNotEmpty() }
    }

    private fun computePinyinInitial(c: Char): String? {
        if (c in 'a'..'z' || c in 'A'..'Z') return c.uppercaseChar().toString()
        val tl = transliterator ?: return "#"
        val latin = runCatching {
            synchronized(tl) {
                tl.transliterate(c.toString())
            }
        }.getOrNull() ?: return "#"
        if (latin == c.toString()) return "#"
        val first = latin.firstOrNull { it in 'a'..'z' || it in 'A'..'Z' } ?: return "#"
        return first.uppercaseChar().toString()
    }

    fun comparePinyin(a: String, b: String): Int {
        return collator.get()!!.compare(a, b)
    }
}
