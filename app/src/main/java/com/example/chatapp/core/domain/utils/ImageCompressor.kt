package com.example.chatapp.core.domain.utils

import android.net.Uri
import com.example.chatapp.core.domain.util.ImageCompressionError
import com.example.chatapp.core.domain.util.Result

interface ImageCompressor {
    suspend fun compressImage(
        contentUri: Uri,
        compressionThreshold: Long
    ): Result<ByteArray, ImageCompressionError>
}