package com.noctplayer.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.noctplayer.app.data.local.db.dao.FavoriteDao
import com.noctplayer.app.data.local.db.dao.MediaDao
import com.noctplayer.app.data.local.db.dao.PlaylistDao
import com.noctplayer.app.data.local.db.dao.WatchProgressDao
import com.noctplayer.app.data.local.db.entity.FavoriteEntity
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.PlaylistEntity
import com.noctplayer.app.data.local.db.entity.PlaylistItemEntity
import com.noctplayer.app.data.local.db.entity.WatchProgressEntity

@Database(
    entities = [
        MediaItemEntity::class,
        WatchProgressEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noctplayer.db"
                ).build().also { INSTANCE = it }
            }
    }
}
