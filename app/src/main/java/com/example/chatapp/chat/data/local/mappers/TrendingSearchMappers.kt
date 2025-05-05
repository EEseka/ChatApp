package com.example.chatapp.chat.data.local.mappers

import com.example.chatapp.chat.data.local.entities.TrendingSearchEntity

fun List<String>.toTrendingSearchEntity(): TrendingSearchEntity {
    return TrendingSearchEntity(
        trendingTopics = this,
        timestamp = System.currentTimeMillis()
    )
}

fun TrendingSearchEntity.toTrendingSearch(): List<String> {
    return this.trendingTopics
}