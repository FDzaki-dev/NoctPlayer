package com.noctplayer.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "mediaStoreId"])
data class PlaylistItemEntity(
    val playlistId: Long,
    val mediaStoreId: Long,
    val orderIndex: Int
)
