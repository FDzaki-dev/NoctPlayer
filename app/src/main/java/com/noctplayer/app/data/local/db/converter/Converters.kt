package com.noctplayer.app.data.local.db.converter

import androidx.room.TypeConverter
import com.noctplayer.app.data.local.db.entity.MediaSource

class Converters {
    @TypeConverter
    fun fromMediaSource(value: MediaSource): String = value.name

    @TypeConverter
    fun toMediaSource(value: String): MediaSource = MediaSource.valueOf(value)
}
