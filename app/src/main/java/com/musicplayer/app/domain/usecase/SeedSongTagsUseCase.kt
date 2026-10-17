package com.musicplayer.app.domain.usecase

import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.domain.model.Source
import java.time.Year
import javax.inject.Inject

class SeedSongTagsUseCase @Inject constructor(
    private val songDao: SongDao
) {
    suspend operator fun invoke() {
        val currentYear = Year.now().value
        songDao.getSongs()
            .asSequence()
            .filter { it.source == Source.LOCAL }
            .filter { it.semanticTags.isNullOrBlank() }
            .forEach { song ->
                val tags = inferTags(song, currentYear)
                if (tags.isNotEmpty()) {
                    songDao.updateSemanticTags(song.id, tags.joinToString("|"))
                }
            }
    }

    private fun inferTags(song: SongEntity, currentYear: Int): List<String> {
        val tags = LinkedHashSet<String>()
        tags += languageTag(song)

        song.releaseYear
            ?.takeIf { it in 1900..currentYear }
            ?.let { year ->
                tags += when {
                    year <= 1999 -> "经典老歌"
                    year <= 2009 -> "千禧年代"
                    year <= 2019 -> "2010年代"
                    else -> "新歌速递"
                }
            }

        val text = "${song.title} ${song.albumTitle} ${song.artistName}".lowercase()
        GENRE_KEYWORDS.forEach { (tag, keywords) ->
            if (keywords.any { text.contains(it) }) tags += tag
        }

        if (song.isFavorite) tags += "收藏"
        if (tags.isEmpty()) tags += "其他"
        return tags.take(6)
    }

    private fun languageTag(song: SongEntity): String {
        val text = song.title + song.artistName + song.albumTitle
        return when {
            text.any { it in KANA_RANGE } -> "日语"
            text.any { it in HANGUL_RANGE } -> "韩语"
            text.any { it.isChineseChar() } -> {
                if (song.artistName.trim() in CANTONESE_ARTISTS) "粤语经典" else "华语流行"
            }
            else -> "欧美流行"
        }
    }

    private fun Char.isChineseChar(): Boolean = this in '\u4E00'..'\u9FFF'

    private companion object {
        val KANA_RANGE = '\u3040'..'\u30FF'
        val HANGUL_RANGE = '\uAC00'..'\uD7AF'

        val CANTONESE_ARTISTS = setOf(
            "陈奕迅", "张国荣", "梅艳芳", "谭咏麟", "李克勤", "容祖儿", "杨千嬅",
            "Twins", "Beyond", "林子祥", "叶倩文", "王菲", "郑秀文", "古巨基",
            "张敬轩", "谢安琪", "陈慧娴", "关淑怡", "黄耀明", "达明一派", "草蜢",
            "刘德华", "张学友", "黎明", "郭富城", "许冠杰", "罗文", "甄妮", "汪明荃"
        )

        val GENRE_KEYWORDS = linkedMapOf(
            "现场" to listOf("live", "现场", "演唱会", "concert"),
            "纯音乐" to listOf("instrumental", "纯音乐", "钢琴", "piano", "伴奏"),
            "电子" to listOf("remix", "edm", "电子", "dj ", "club mix"),
            "摇滚" to listOf("rock", "摇滚", "乐队", "band"),
            "说唱" to listOf("rap", "hip-hop", "hiphop", "说唱"),
            "爵士" to listOf("jazz", "爵士"),
            "原声" to listOf("ost", "原声", "电视", "电影", "soundtrack"),
            "民谣" to listOf("folk", "民谣"),
            "轻音乐" to listOf("轻音乐", "ambient", "放松", "lofi", "lo-fi")
        )
    }
}
