package com.example.chatapp.authentication.presentation.signup

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Person2
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R
import com.example.chatapp.authentication.presentation.components.PhotoActionButton
import com.example.chatapp.core.presentation.CameraAndGalleryPermissionHandler
import com.example.chatapp.ui.theme.ChatAppTheme
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.ImageCropperDialog

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
                        ),
                    )
                },
                dialogPadding = PaddingValues(0.dp)
            )
        }

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