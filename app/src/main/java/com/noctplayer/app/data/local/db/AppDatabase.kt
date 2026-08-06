package com.noctplayer.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.noctplayer.app.data.local.db.converter.Converters
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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
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
                )
                    // v1->v2: primary key changed from Long mediaStoreId to String id
                    // (dual MediaStore/SAF source support). No shipped users yet on
                    // v1, so destructive migration is acceptable here — a real
                    // migration path should replace this before any public release.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
