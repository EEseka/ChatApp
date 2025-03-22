package com.example.chatapp.core.domain

import android.net.Uri

interface FileManager {
    suspend fun saveImageToCache(bytes: ByteArray, fileName: String): Uri?
    suspend fun clearCache()
}