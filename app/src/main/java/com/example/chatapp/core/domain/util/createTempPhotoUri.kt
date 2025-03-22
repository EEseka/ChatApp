package com.example.chatapp.core.domain.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Extension function to create temporary photo URI
suspend fun Context.createTempPhotoUri(): Uri {
    return withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile(
            "profile_photo_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )
        FileProvider.getUriForFile(
            this@createTempPhotoUri,
            "${packageName}.fileprovider",
            tempFile
        )
    }
}