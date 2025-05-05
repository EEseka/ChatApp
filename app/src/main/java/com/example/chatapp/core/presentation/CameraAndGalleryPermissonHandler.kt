package com.example.chatapp.core.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.chatapp.R
import com.example.chatapp.core.domain.util.createTempPhotoUri
import com.example.chatapp.core.domain.util.toUri
import com.mr0xf00.easycrop.CropError
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.ImageCropper
import com.mr0xf00.easycrop.crop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CameraAndGalleryPermissionHandler(
    context: Context,
    scope: CoroutineScope,
    imageCropper: ImageCropper,
    checkAndLaunchCamera: Boolean,
    checkAndLaunchGallery: Boolean,
    changeCheckAndLaunchCamera: (Boolean) -> Unit,
    changeCheckAndLaunchGallery: (Boolean) -> Unit,
    changeIsCropping: (Boolean) -> Unit,
    onPhotoSelected: (Uri, String) -> Unit,
) {
    var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var galleryPermissionRequested by rememberSaveable { mutableStateOf(false) }

    var cameraPermissionGranted by rememberSaveable { mutableStateOf(false) }
    var galleryPermissionGranted by rememberSaveable { mutableStateOf(false) }
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        cameraPermissionRequested = false

        if (success && tempPhotoUri != null) {
            // First show un-cropped version
            val mimeType = context.contentResolver.getType(tempPhotoUri!!)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                onPhotoSelected(tempPhotoUri!!, extension)
            }

            // Then attempt to crop
            changeIsCropping(true)
            scope.launch {
                val result = imageCropper.crop(tempPhotoUri!!, context)
                when (result) {
                    CropResult.Cancelled -> {
                        changeIsCropping(false)
                    }

                    is CropError -> {
                        changeIsCropping(false)
                        Log.e(
                            "CameraAndGalleryPermissionHandler",
                            "Error cropping image: $result"
                        )
                    }

                    is CropResult.Success -> {
                        val croppedUri = result.bitmap.toUri(context, tempPhotoUri!!)
                        if (croppedUri != null) {
                            tempPhotoUri = croppedUri
                            // Update with cropped version
                            if (extension != null) {
                                onPhotoSelected(croppedUri, extension)
                            }
                        }
                        changeIsCropping(false)
                    }
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { contentUri ->
        galleryPermissionRequested = false
        contentUri?.let { uri ->
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                onPhotoSelected(uri, extension)
            }
        }
    }

    fun checkAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
            scope.launch {
                tempPhotoUri = context.createTempPhotoUri()
                tempPhotoUri?.let { uri -> cameraLauncher.launch(uri) }
            }
        } else {
            cameraPermissionRequested = true
        }
    }

    fun checkAndLaunchGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // No need to request READ_MEDIA_IMAGES permission; just launch the photo picker
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            val readPermission = Manifest.permission.READ_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(context, readPermission) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                galleryPermissionGranted = true
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                galleryPermissionRequested = true
            }
        }
    }

    // Permission handler for camera and gallery
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle camera permission result
        permissions[Manifest.permission.CAMERA]?.let { granted ->
            if (granted) {
                cameraPermissionGranted = true
                if (cameraPermissionRequested) {
                    scope.launch {
                        tempPhotoUri = context.createTempPhotoUri()
                        tempPhotoUri?.let { uri -> cameraLauncher.launch(uri) }
                    }
                }
            } else if (cameraPermissionRequested) {
                // Reset the flag even if permission is denied
                cameraPermissionRequested = false

                Toast.makeText(
                    context,
                    context.getString(R.string.camera_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Handle gallery permission result
        val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES]
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
        }

        galleryPermission?.let { granted ->
            if (granted) {
                galleryPermissionGranted = true
                if (galleryPermissionRequested) {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            } else if (galleryPermissionRequested) {
                // Reset the flag even if permission is denied
                galleryPermissionRequested = false

                Toast.makeText(
                    context,
                    context.getString(R.string.gallery_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Permission Request Trigger
    LaunchedEffect(cameraPermissionRequested, galleryPermissionRequested) {
        if (cameraPermissionRequested || galleryPermissionRequested) {
            val permissions = mutableListOf<String>()

            if (cameraPermissionRequested) {
                permissions.add(Manifest.permission.CAMERA)
            }

            if (galleryPermissionRequested) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }
    LaunchedEffect(checkAndLaunchCamera) {
        if (checkAndLaunchCamera) {
            checkAndLaunchCamera()
            changeCheckAndLaunchCamera(false)
        }
    }
    LaunchedEffect(checkAndLaunchGallery) {
        if (checkAndLaunchGallery) {
            checkAndLaunchGallery()
            changeCheckAndLaunchGallery(false)
        }
    }
}