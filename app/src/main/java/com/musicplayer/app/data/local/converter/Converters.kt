package com.musicplayer.app.data.local.converter

import androidx.room.TypeConverter
import com.musicplayer.app.domain.model.Source

class Converters {
    @TypeConverter
    fun sourceToString(source: Source): String = source.name

    @TypeConverter
    fun stringToSource(value: String): Source {
        return runCatching { Source.valueOf(value) }.getOrDefault(Source.LOCAL)
    }
}
