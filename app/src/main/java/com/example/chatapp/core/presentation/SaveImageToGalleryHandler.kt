package com.example.chatapp.core.presentation

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.chatapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

private const val TAG = "SaveImageToGalleryHandler"

@Composable
fun SaveImageToGalleryHandler(
    imageUri: Uri,
    saveIcon: ImageVector,
    modifier: Modifier = Modifier,
    onSaveStarted: () -> Unit = {},
    onSaveComplete: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savePermissionRequested by rememberSaveable { mutableStateOf(false) }

    // Permission launcher for Android 9 and below
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        savePermissionRequested = false
        if (isGranted) {
            scope.launch {
                onSaveStarted()
                saveImageToGallery(context, imageUri)
                    .onSuccess {
                        Toast.makeText(
                            context,
                            R.string.image_saved_successfully,
                            Toast.LENGTH_SHORT
                        ).show()
                        onSaveComplete()
                    }
                    .onFailure { e ->
                        Toast.makeText(
                            context,
                            R.string.error_saving_image,
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Error saving image: ${e.message}")
                        onSaveComplete()
                    }
            }
        } else {
            Toast.makeText(context, R.string.permission_required_to_save_image, Toast.LENGTH_SHORT)
                .show()
            onSaveComplete()
        }
    }

    IconButton(
        onClick = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) -> {
                        scope.launch {
                            onSaveStarted()
                            saveImageToGallery(context, imageUri)
                                .onSuccess {
                                    Toast.makeText(
                                        context,
                                        R.string.image_saved_successfully,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSaveComplete()
                                }
                                .onFailure { e ->
                                    Toast.makeText(
                                        context,
                                        R.string.error_saving_image,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e(TAG, "Error saving image: ${e.message}")
                                    onSaveComplete()
                                }
                        }
                    }

                    else -> {
                        savePermissionRequested = true
                    }
                }
            } else {
                // Android 10 and above don't need permission
                scope.launch {
                    onSaveStarted()
                    saveImageToGallery(context, imageUri)
                        .onSuccess {
                            Toast.makeText(
                                context,
                                R.string.image_saved_successfully,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            onSaveComplete()
                        }
                        .onFailure { e ->
                            Toast.makeText(
                                context,
                                R.string.error_saving_image,
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(TAG, "Error saving image: ${e.message}")
                            onSaveComplete()
                        }
                }
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = saveIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }

    // Permission Request Trigger
    LaunchedEffect(savePermissionRequested) {
        if (savePermissionRequested) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

private suspend fun saveImageToGallery(context: Context, uri: Uri): Result<Unit> =
    withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val displayName = "ChatApp_$timestamp.png"

            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val imageDetails = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ChatApp")
                }
            }

            val contentResolver = context.contentResolver
            val savedImageUri = contentResolver.insert(imageCollection, imageDetails)
                ?: throw IOException("Failed to create new MediaStore record.")

            contentResolver.openOutputStream(savedImageUri)?.use { outputStream ->
                // For Firebase Storage URLs, use URL connection instead of ContentResolver
                if (uri.scheme == "https") {
                    val connection = java.net.URL(uri.toString()).openConnection()
                    connection.connect()
                    connection.getInputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } else {
                    // For local content URIs, use ContentResolver
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    } ?: throw IOException("Failed to open input stream")
                }
            } ?: throw IOException("Failed to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(savedImageUri, imageDetails, null, null)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }