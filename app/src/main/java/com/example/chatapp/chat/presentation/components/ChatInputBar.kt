package com.example.chatapp.chat.presentation.components

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.chat.domain.models.Mood
import com.example.chatapp.chat.presentation.utils.getMoodEmoji
import com.example.chatapp.core.presentation.AudioRecordingPermissionHandler
import com.example.chatapp.ui.theme.ChatAppTheme

@Composable
fun ChatInputBar(
    currentInput: String,
    selectedMood: Mood,
    isTyping: Boolean,
    isRecording: Boolean,
    isSelectingImage: Boolean,
    isTranscribing: Boolean,
    @StringRes error: Int? = null,
    isEditingMessage: Boolean,
    isReasoningEnabled: Boolean,
    isOnlineSearchEnabled: Boolean,
    isGenerateImageEnabled: Boolean,
    onInputChanged: (String) -> Unit,
    onRemoveImage: () -> Unit,
    selectedImageUri: Uri?,
    onMoodSelected: (Mood) -> Unit,
    onToggleReasoning: () -> Unit,
    onToggleOnlineSearch: () -> Unit,
    onToggleImageGeneration: () -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    onCancelAudioRecording: () -> Unit,
    onSendMessage: () -> Unit,
    onSendEditedMessage: () -> Unit,
    onCancelEdit: () -> Unit,
    changeCheckAndLaunchCameraToTrue: () -> Unit,
    changeCheckAndLaunchGalleryToTrue: () -> Unit
) {
    var isMediaDropdownExpanded by remember { mutableStateOf(false) }
    var isMoodDropdownExpanded by remember { mutableStateOf(false) }
    var showImageGenInputBar by rememberSaveable { mutableStateOf(false) }

    if (isRecording) {
        AudioBar(
            isTranscribing = isTranscribing,
            onStopRecording = onStopAudioRecording,
            onCancelRecording = onCancelAudioRecording,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        if (showImageGenInputBar) {
            ImageGenInputBar(
                value = currentInput,
                isTyping = isTyping,
                error = error,
                isEditingMessage = isEditingMessage,
                onCancelEdit = onCancelEdit,
                onToggleImageGeneration = {
                    showImageGenInputBar = false
                    onToggleImageGeneration()
                },
                onValueChange = { onInputChanged(it) },
                onVoiceInput = onStartAudioRecording,
                onSendMessage = onSendMessage,
                onSendEditedMessage = onSendEditedMessage
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Selected Image Preview
                selectedImageUri?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .wrapContentSize()
                            .height(150.dp)
                            .padding(8.dp)
                    ) {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { isMoodDropdownExpanded = true },
                            label = { Text("${getMoodEmoji(selectedMood)}   ${selectedMood.name}") }
                        )
                        DropdownMenu(
                            expanded = isMoodDropdownExpanded,
                            onDismissRequest = { isMoodDropdownExpanded = false }
                        ) {
                            Mood.entries.forEach { mood ->
                                DropdownMenuItem(
                                    onClick = {
                                        isMoodDropdownExpanded = false
                                        onMoodSelected(mood)
                                    },
                                    text = { Text("${getMoodEmoji(mood)}    ${mood.name}") }
                                )
                            }
                        }
                    }
                    FilterChip(
                        selected = isOnlineSearchEnabled,
                        onClick = onToggleOnlineSearch,
                        label = { Text("Search") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                            )
                        }
                    )
                    FilterChip(
                        selected = isReasoningEnabled,
                        onClick = onToggleReasoning,
                        label = { Text("Reason") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                            )
                        }
                    )
                    FilterChip(
                        selected = isGenerateImageEnabled,
                        onClick = {
                            showImageGenInputBar = !showImageGenInputBar
                            onToggleImageGeneration()
                        },
                        label = { Text(stringResource(R.string.generate_image)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = null,
                            )
                        }
                    )
                }
                // Text Input
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = error?.let {
                        { Text(text = stringResource(it), color = MaterialTheme.colorScheme.error) }
                    },
                    value = currentInput,
                    onValueChange = { onInputChanged(it) },
                    placeholder = { Text(stringResource(R.string.ask_anything)) },
                    maxLines = 5,
                    leadingIcon = {
                        IconButton(
                            onClick = { isMediaDropdownExpanded = !isMediaDropdownExpanded },
                            enabled = !isSelectingImage
                        ) {
                            if (isSelectingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = isMediaDropdownExpanded,
                            onDismissRequest = { isMediaDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.camera)) },
                                onClick = {
                                    isMediaDropdownExpanded = false
                                    changeCheckAndLaunchCameraToTrue()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.image)) },
                                onClick = {
                                    isMediaDropdownExpanded = false
                                    changeCheckAndLaunchGalleryToTrue()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            AudioRecordingPermissionHandler(
                                micIcon = Icons.Default.Mic,
                                onStartRecording = onStartAudioRecording
                            )
                            val sendAction =
                                if (isEditingMessage) onSendEditedMessage else onSendMessage
                            IconButton(
                                onClick = sendAction,
                                enabled = currentInput.isNotBlank() && !isTyping
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    label = if (isEditingMessage) {
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.editing))
                                IconButton(onClick = onCancelEdit) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    } else null,
                )
            }
        }
    }
}

@Composable
fun AudioBar(
    isTranscribing: Boolean,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancelRecording) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated recording indicator
            val infiniteTransition = rememberInfiniteTransition(label = "recording")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.recording),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isTranscribing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
        } else {
            IconButton(onClick = onStopRecording) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ImageGenInputBar(
    value: String,
    error: Int?,
    isTyping: Boolean,
    isEditingMessage: Boolean,
    onCancelEdit: () -> Unit,
    onToggleImageGeneration: () -> Unit,
    onValueChange: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onSendMessage: () -> Unit,
    onSendEditedMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilterChip(
            selected = true,
            onClick = onToggleImageGeneration,
            label = { Text(stringResource(R.string.generate_image)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = null,
                )
            }
        )
        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            shape = MaterialTheme.shapes.medium,
            value = value,
            supportingText = error?.let {
                { Text(text = stringResource(it), color = MaterialTheme.colorScheme.error) }
            },
            onValueChange = { onValueChange(it) },
            placeholder = { Text(stringResource(R.string.describe_image_gen)) },
            maxLines = 5,
            trailingIcon = {
                Row {
                    AudioRecordingPermissionHandler(
                        micIcon = Icons.Default.Mic,
                        onStartRecording = onVoiceInput
                    )
                    val sendAction = if (isEditingMessage) onSendEditedMessage else onSendMessage
                    IconButton(
                        onClick = sendAction,
                        enabled = value.isNotBlank() && !isTyping,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null
                        )
                    }
                }
            },
            label = if (isEditingMessage) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.editing))
                        IconButton(onClick = onCancelEdit) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }
                }
            } else null,
        )
    }
}

@PreviewLightDark
@Composable
fun ChatInputBarPreview() {
    ChatAppTheme {
        ChatInputBar(
            currentInput = "",
            selectedMood = Mood.FRIENDLY,
            isSelectingImage = false,
            error = null,
            isReasoningEnabled = true,
            isTranscribing = false,
            isOnlineSearchEnabled = false,
            isGenerateImageEnabled = true,
            onInputChanged = {},
            onRemoveImage = {},
            selectedImageUri = null,
            onMoodSelected = {},
            onToggleReasoning = {},
            onToggleOnlineSearch = {},
            onToggleImageGeneration = {},
            onSendMessage = {},
            changeCheckAndLaunchCameraToTrue = {},
            changeCheckAndLaunchGalleryToTrue = {},
            isEditingMessage = true,
            onCancelEdit = {},
            onSendEditedMessage = {},
            isTyping = false,
            isRecording = true,
            onStartAudioRecording = {},
            onStopAudioRecording = {},
            onCancelAudioRecording = {}
        )
    }
}