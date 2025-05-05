package com.example.chatapp.core.domain.utils

import android.net.Uri
import java.io.File

interface FileManager {
    suspend fun saveImageToCache(bytes: ByteArray, fileName: String): Uri?
    suspend fun createFile(fileName: String, taskToPerformAfterCreation: (File) -> Unit): File?
    suspend fun downloadImageFromUrl(imageUrl: String): ByteArray?
    suspend fun clearCache()
}