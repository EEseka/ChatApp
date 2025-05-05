package com.example.chatapp.core.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun ImageBitmap.toUri(context: Context, originalUri: Uri?): Uri? {
    val bitmap = this.asAndroidBitmap() // Convert ImageBitmap to Bitmap

    // Get MIME type from the original URI or default to "image/jpeg"
    val mimeType = originalUri?.let { uri ->
        context.contentResolver.getType(uri)
    } ?: "image/jpeg" // Default to JPEG if unknown

    // Map MIME type to file extension, default to "jpg" if not found
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

    // Determine the compression format based on the file extension
    val format = when (extension) {
        "png" -> Bitmap.CompressFormat.PNG
        "webp" -> Bitmap.CompressFormat.WEBP // Added support for WEBP format
        else -> Bitmap.CompressFormat.JPEG // Default to JPEG for other formats
    }

    // Create a temporary file in the cache directory with the appropriate extension
    val file = File(context.cacheDir, "cropped_image_${System.currentTimeMillis()}.$extension")
    return try {
        FileOutputStream(file).use { outputStream ->
            // Compress the bitmap into the file with 100% quality
            bitmap.compress(format, 100, outputStream)
        }
        // Return a content URI for the created file
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace() // Log the exception for debugging
        null // Return null if an error occurs
    }
}