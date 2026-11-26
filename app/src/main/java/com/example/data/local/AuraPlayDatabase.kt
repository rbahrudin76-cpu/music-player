package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaTrackEntity::class,
        LinkedFolderEntity::class,
        CustomEqPresetEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuraPlayDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AuraPlayDatabase? = null

        fun getInstance(context: Context): AuraPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraPlayDatabase::class.java,
                    "auraplay_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
