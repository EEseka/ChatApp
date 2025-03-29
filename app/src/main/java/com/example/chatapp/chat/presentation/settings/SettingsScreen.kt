package com.example.chatapp.chat.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R
import com.example.chatapp.chat.presentation.components.EditableField
import com.example.chatapp.chat.presentation.components.PhotoActionButton
import com.example.chatapp.core.domain.util.createTempPhotoUri
import com.example.chatapp.ui.theme.ChatAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onDisplayNameChanged: (TextFieldValue) -> Unit,
    onUpdateProfileClicked: () -> Unit,
    onDeleteAccountClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onPhotoSelected: (Uri, String) -> Unit,
    onScreenLeave: () -> Unit,
    onScreenReturn: () -> Unit
) {
    // Editable Name and Pfp State restoration shenanigans
    LaunchedEffect(Unit) {
        onScreenReturn()
    }
    DisposableEffect(Unit) {
        onDispose {
            onScreenLeave()
        }
    }
    // Permission Shenanigans
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    // Screen starts here
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

    var isEditingDisplayName by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val displayNameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditingDisplayName) {
        if (isEditingDisplayName) {
            displayNameFocusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(title = { Text(stringResource(R.string.settings)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Section
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            Log.d("SettingsScreen", "PhotoUri: ${state.photoUri}")
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
                    Spacer(modifier = Modifier.height(8.dp))
                    // Display Name Input
                    EditableField(
                        label = stringResource(R.string.display_name),
                        value = state.displayName,
                        isError = state.displayNameError != null,
                        supportingText = state.displayNameError,
                        onValueChange = { onDisplayNameChanged(it) },
                        isEditing = isEditingDisplayName,
                        onEditClick = { isEditingDisplayName = true },
                        onSaveClick = { isEditingDisplayName = false },
                        isLoading = state.isProfileUpdating,
                        focusRequester = displayNameFocusRequester,
                        focusManager = focusManager
                    )
                    //Email (Non editable)
                    Text(
                        text = state.email,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Update Profile Button
            FilledTonalButton(
                onClick = {
                    onUpdateProfileClicked()
                    focusManager.clearFocus()
                    isEditingDisplayName = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isProfileUpdating
            ) {
                if (state.isProfileUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(stringResource(R.string.update_profile))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.account_actions),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSigningOut) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(stringResource(R.string.sign_out))
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (state.isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(stringResource(R.string.delete_account))
            }
        }

        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.sign_out)) },
                text = { Text(stringResource(R.string.sign_out_confirmation)) },
                onDismissRequest = { showSignOutDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSignOutClicked()
                            showSignOutDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Delete Account Confirmation Dialog
        if (showDeleteAccountDialog) {
            AlertDialog(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.delete_account)) },
                text = {
                    Text(stringResource(R.string.delete_account_confirmation))
                },
                onDismissRequest = { showDeleteAccountDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteAccountClicked()
                            showDeleteAccountDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.delete_account))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    ChatAppTheme {
        SettingsScreen(
            state = SettingsState(
                displayName = TextFieldValue("John Pork"),
                email = "johnpork@gmail.com",
                photoUriError = R.string.error_unknown
            ),
            onDisplayNameChanged = {},
            onUpdateProfileClicked = {},
            onDeleteAccountClicked = {},
            onSignOutClicked = {},
            onPhotoSelected = { uri, extension -> },
            onScreenLeave = {},
            onScreenReturn = {}
        )
    }
}