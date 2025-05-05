package com.example.chatapp.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chatapp.chat.data.local.entities.TrendingSearchEntity

@Database(
    entities = [TrendingSearchEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrendingSearchDatabase : RoomDatabase() {
    abstract val dao: TrendingSearchDao
}