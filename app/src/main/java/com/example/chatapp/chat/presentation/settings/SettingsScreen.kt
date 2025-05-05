package com.example.chatapp.chat.presentation.settings

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.chatapp.R
import com.example.chatapp.chat.presentation.components.EditableField
import com.example.chatapp.chat.presentation.components.PhotoActionButton
import com.example.chatapp.core.presentation.CameraAndGalleryPermissionHandler
import com.example.chatapp.core.presentation.components.shimmerEffect
import com.example.chatapp.ui.theme.ChatAppTheme
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.ImageCropperDialog

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
    onScreenReturn: () -> Unit,
    onSetActivityContext: (ComponentActivity) -> Unit,
    onClearActivityContext: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onSetActivityContext(context as ComponentActivity)
    }
    DisposableEffect(Unit) {
        onDispose {
            onClearActivityContext()
        }
    }
    // Editable Name and Pfp State restoration shenanigans
    LaunchedEffect(Unit) {
        onScreenReturn()
    }
    DisposableEffect(Unit) {
        onDispose {
            onScreenLeave()
        }
    }

    val scope = rememberCoroutineScope()
    val imageCropper = rememberImageCropper()
    var isCropping by rememberSaveable { mutableStateOf(false) }

    var checkAndLaunchCamera by rememberSaveable { mutableStateOf(false) }
    var checkAndLaunchGallery by rememberSaveable { mutableStateOf(false) }

    CameraAndGalleryPermissionHandler(
        context = context,
        scope = scope,
        imageCropper = imageCropper,
        checkAndLaunchCamera = checkAndLaunchCamera,
        checkAndLaunchGallery = checkAndLaunchGallery,
        changeCheckAndLaunchCamera = { checkAndLaunchCamera = it },
        changeCheckAndLaunchGallery = { checkAndLaunchGallery = it },
        changeIsCropping = { isCropping = it },
        onPhotoSelected = { uri, extension ->
            onPhotoSelected(uri, extension)
        }
    )

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
            val cropState = imageCropper.cropState
            if (isCropping && cropState != null) {
                ImageCropperDialog(
                    state = cropState,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.crop_image)) },
                            navigationIcon = {
                                IconButton(onClick = { cropState.done(accept = false) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { cropState.reset() }) {
                                    Icon(Icons.Default.Restore, null)
                                }
                                IconButton(
                                    onClick = { cropState.done(accept = true) },
                                    enabled = !cropState.accepted
                                ) {
                                    Icon(Icons.Default.Done, null)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    dialogPadding = PaddingValues(0.dp),
                )
            }
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
                            if (state.photoUri != null) {
                                val painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(state.photoUri)
                                        .crossfade(true)
                                        .build()
                                )
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (painter.state is AsyncImagePainter.State.Loading) {
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .shimmerEffect())
                                    }
                                    Image(
                                        painter = painter,
                                        contentDescription = stringResource(R.string.profile_photo),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
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
                            onClick = { checkAndLaunchCamera = true },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 8.dp)
                        )

                        PhotoActionButton(
                            icon = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.choose_from_gallery),
                            onClick = { checkAndLaunchGallery = true },
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
            onScreenReturn = {},
            onSetActivityContext = {},
            onClearActivityContext = {}
        )
    }
}