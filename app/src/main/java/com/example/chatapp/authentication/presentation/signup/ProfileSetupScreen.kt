package com.example.chatapp.authentication.presentation.signup

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Person2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R
import com.example.chatapp.authentication.presentation.components.PhotoActionButton
import com.example.chatapp.core.domain.util.createTempPhotoUri
import com.example.chatapp.ui.theme.ChatAppTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileSetupScreen(
    state: SignUpState,
    onDisplayNameChanged: (String) -> Unit,
    onPhotoSelected: (Uri, String) -> Unit,
    onSaveProfileClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var galleryPermissionRequested by remember { mutableStateOf(false) }

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var galleryPermissionGranted by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        cameraPermissionRequested = false

        if (success && tempPhotoUri != null) {
            val mimeType = context.contentResolver.getType(tempPhotoUri!!)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                onPhotoSelected(tempPhotoUri!!, extension)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.setup_your_profile),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (state.photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.profile_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PhotoActionButton(
                icon = Icons.Rounded.CameraAlt,
                contentDescription = stringResource(R.string.take_photo),
                onClick = { checkAndLaunchCamera() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp)
            )

            PhotoActionButton(
                icon = Icons.Rounded.AddPhotoAlternate,
                contentDescription = stringResource(R.string.choose_from_gallery),
                onClick = { checkAndLaunchGallery() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp)
            )
        }

        AnimatedVisibility(state.photoUriError != null) {
            Text(
                text = stringResource(state.photoUriError!!),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = { onDisplayNameChanged(it) },
            label = { Text(stringResource(R.string.display_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.displayNameError != null,
            supportingText = state.displayNameError?.let {
                { Text(stringResource(it)) }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Person2,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onSaveProfileClicked()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.displayName.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(stringResource(R.string.save_profile))
        }
    }
}

@Preview(
    showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpScreenPreview() {
    ChatAppTheme {
        ProfileSetupScreen(
            state = SignUpState(),
            onDisplayNameChanged = {},
            onPhotoSelected = { _, _ -> },
            onSaveProfileClicked = {}
        )
    }
}

// TODO: "Pressing done on the keyboard in the name text field launches camera"