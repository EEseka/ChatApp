package com.example.chatapp.chat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatapp.chat.data.local.entities.TrendingSearchEntity

@Dao
interface TrendingSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendingSearches(searches: TrendingSearchEntity)

    @Query("SELECT * FROM trending_searches ")
    suspend fun getTrendingSearches(): TrendingSearchEntity?

    @Query("DELETE FROM trending_searches")
    suspend fun clearTrendingSearches()
}