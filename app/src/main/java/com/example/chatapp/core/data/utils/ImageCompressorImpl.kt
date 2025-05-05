package com.example.chatapp.core.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.example.chatapp.core.domain.utils.ImageCompressor
import com.example.chatapp.core.domain.util.ImageCompressionError
import com.example.chatapp.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class ImageCompressorImpl(private val context: Context) : ImageCompressor {
    override suspend fun compressImage(
        contentUri: Uri,
        compressionThreshold: Long
    ): Result<ByteArray, ImageCompressionError> {
        return withContext(Dispatchers.IO) {
            val inputBytes = try {
                context.contentResolver.openInputStream(contentUri)?.use { it.readBytes() }
                    ?: return@withContext Result.Error(ImageCompressionError.FILE_NOT_FOUND)
            } catch (e: IOException) {
                return@withContext Result.Error(ImageCompressionError.FILE_IO_ERROR)
            }

            ensureActive()

            val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
                ?: return@withContext Result.Error(ImageCompressionError.FILE_NOT_IMAGE)

            ensureActive()

            val mimeType = context.contentResolver.getType(contentUri)
            val compressFormat = determineCompressFormat(mimeType)

            return@withContext compressBitmap(bitmap, compressFormat, compressionThreshold)
        }
    }

    private fun determineCompressFormat(mimeType: String?): Bitmap.CompressFormat {
        return when (mimeType) {
            "image/png" -> Bitmap.CompressFormat.PNG
            "image/jpeg" -> Bitmap.CompressFormat.JPEG
            "image/webp" -> if (Build.VERSION.SDK_INT >= 30) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else Bitmap.CompressFormat.WEBP

            else -> Bitmap.CompressFormat.JPEG
        }
    }

    private suspend fun compressBitmap(
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        compressionThreshold: Long
    ): Result<ByteArray, ImageCompressionError> = withContext(Dispatchers.Default) {
        var outputBytes: ByteArray
        var quality = 90

        try {
            do {
                ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(compressFormat, quality, outputStream)
                    outputBytes = outputStream.toByteArray()
                    quality -= (quality * 0.1).roundToInt()
                }
            } while (isActive &&
                outputBytes.size > compressionThreshold &&
                quality > 5 &&
                compressFormat != Bitmap.CompressFormat.PNG
            )

            if (outputBytes.isEmpty()) {
                return@withContext Result.Error(ImageCompressionError.COMPRESSION_ERROR)
            }

            Result.Success(outputBytes)
        } catch (e: Exception) {
            Result.Error(ImageCompressionError.COMPRESSION_ERROR)
        }
    }
}