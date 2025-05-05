package com.example.chatapp.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trending_searches")
data class TrendingSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trendingTopics: List<String>,
    val timestamp: Long
)