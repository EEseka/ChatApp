package com.example.chatapp.core.data.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.chatapp.core.domain.utils.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL

class FileManagerImpl(private val context: Context) : FileManager {

    override suspend fun saveImageToCache(bytes: ByteArray, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, fileName)
                file.outputStream().use { outputStream ->
                    outputStream.write(bytes)
                }
                Uri.fromFile(file)  // Return the URI of the saved file
            } catch (e: IOException) {
                Log.w(TAG, "Error saving image to cache: ${e.printStackTrace()}")
                null
            }
        }
    }

    override suspend fun createFile(
        fileName: String,
        taskToPerformAfterCreation: (File) -> Unit
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                File(context.cacheDir, fileName).also {
                    taskToPerformAfterCreation(it)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Error creating file: ${e.printStackTrace()}")
                null
            }
        }
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                cacheDir?.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing cache: ${e.printStackTrace()}")
            }
        }
    }

    override suspend fun downloadImageFromUrl(imageUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                URL(imageUrl).openStream().use { input ->
                    input.readBytes()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image", e)
                null
            }
        }
    }

    private companion object {
        private const val TAG = "FileManager"
    }
}