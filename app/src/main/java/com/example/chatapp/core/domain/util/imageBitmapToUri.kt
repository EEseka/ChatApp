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

    val mimeType = originalUri?.let { uri ->
        context.contentResolver.getType(uri)
    } ?: "image/jpeg" // Default to JPEG if unknown

    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    val format = if (extension == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

    val file = File(context.cacheDir, "cropped_image_${System.currentTimeMillis()}.$extension")
    return try {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(format, 100, outputStream) // Preserve quality
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
