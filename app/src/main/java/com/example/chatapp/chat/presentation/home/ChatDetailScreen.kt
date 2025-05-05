package com.example.chatapp.chat.presentation.home

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import com.example.chatapp.chat.domain.models.Chat
import com.example.chatapp.chat.domain.models.ChatPreference
import com.example.chatapp.chat.domain.models.Message
import com.example.chatapp.chat.domain.models.Mood
import com.example.chatapp.chat.domain.models.Role
import com.example.chatapp.chat.presentation.components.AnimatedTypingIndicator
import com.example.chatapp.chat.presentation.components.ChatBubble
import com.example.chatapp.chat.presentation.components.ChatInputBar
import com.example.chatapp.core.domain.util.formatDate
import com.example.chatapp.core.presentation.CameraAndGalleryPermissionHandler
import com.example.chatapp.core.presentation.components.ShimmerChatsItem
import com.example.chatapp.core.presentation.components.shimmerEffect
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.ImageCropperDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatState: ChatState?,
    onSaveTitleEdit: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onTrendingTopicSelected: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onImageSelected: (Uri, String) -> Unit,
    onRemoveImage: () -> Unit,
    onToggleReasoning: () -> Unit,
    onToggleOnlineSearch: () -> Unit,
    onToggleImageGeneration: () -> Unit,
    onMoodSelected: (Mood) -> Unit,
    onEditMessageClicked: (String) -> Unit,
    onEditedMessageSent: (String) -> Unit,
    startAudioRecording: () -> Unit,
    stopAudioRecording: () -> Unit,
    onCancelAudioRecording: () -> Unit,
    onSendMessage: () -> Unit
) {
    val context = LocalContext.current
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
            onImageSelected(uri, extension)
        }
    )

    if (chatState == null) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = glowingAlpha)
            )
            Text(
                text = stringResource(R.string.no_chat_selected),
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = glowingAlpha),
                            MaterialTheme.colorScheme.secondary.copy(alpha = glowingAlpha)
                        )
                    )
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.select_chat_to_view),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val state = chatState
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val focusManager = LocalFocusManager.current
                        val focusRequester = remember { FocusRequester() }
                        var isTitleEditing by rememberSaveable { mutableStateOf(false) }
                        if (isTitleEditing) {
                            AlertDialog(
                                title = { Text(stringResource(R.string.rename_chat)) },
                                text = {
                                    OutlinedTextField(
                                        value = state.chat.title,
                                        onValueChange = { onTitleChanged(it) },
                                        singleLine = true,
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                onSaveTitleEdit()
                                                isTitleEditing = false
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            imeAction = ImeAction.Done
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                    )
                                },
                                onDismissRequest = { isTitleEditing = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            onSaveTitleEdit()
                                            isTitleEditing = false
                                        }
                                    ) {
                                        Text(stringResource(R.string.save))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { isTitleEditing = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.chat.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        isTitleEditing = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.created_at,
                                        formatDate(state.chat.createdAt)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                        }
                    )
                }
                // Trending Topics Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isTrendingLoading) {
                        items(10) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp, 40.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .shimmerEffect(),
                            )
                        }
                    } else {
                        items(state.trendingTopics) { topic ->
                            TextButton(
                                onClick = { onTrendingTopicSelected(topic) },
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    modifier = Modifier.padding(end = 16.dp),
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                if (state.isChatLoading) {
                    repeat(10) {
                        ShimmerChatsItem(modifier = Modifier.padding(16.dp))
                    }
                } else {
                    // Chat Messages
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.chat.messages) {
                        if (state.chat.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.chat.messages.lastIndex)
                        }
                    }
                    var isEditingMessage by rememberSaveable { mutableStateOf(false) }
                    var messageToEditID by rememberSaveable { mutableStateOf("") }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(key = { it.id }, items = state.chat.messages) { message ->
                            ChatBubble(
                                message = message,
                                onEditClicked = {
                                    onEditMessageClicked(it)
                                    isEditingMessage = true
                                    messageToEditID = it
                                }
                            )
                        }
                        if (state.isTyping) {
                            item { AnimatedTypingIndicator(modifier = Modifier.padding(24.dp)) }
                        }
                    }
                    // Chat Input Bar
                    ChatInputBar(
                        currentInput = state.currentInput,
                        onInputChanged = { onInputChanged(it) },
                        isTyping = state.isTyping,
                        isSelectingImage = state.isSelectingImage,
                        isTranscribing = state.isTranscriptionLoading,
                        onRemoveImage = onRemoveImage,
                        selectedImageUri = state.selectedImageUri,
                        isEditingMessage = isEditingMessage,
                        onToggleReasoning = onToggleReasoning,
                        onToggleOnlineSearch = onToggleOnlineSearch,
                        onToggleImageGeneration = onToggleImageGeneration,
                        onSendMessage = onSendMessage,
                        onSendEditedMessage = {
                            onEditedMessageSent(messageToEditID)
                            isEditingMessage = false
                            messageToEditID = ""
                        },
                        error = state.imageCompressionError,
                        isReasoningEnabled = state.chatPreference.isReasoningEnabled,
                        isOnlineSearchEnabled = state.chatPreference.isOnlineSearchEnabled,
                        isGenerateImageEnabled = state.shouldGenerateImage,
                        selectedMood = state.chatPreference.mood,
                        onMoodSelected = { onMoodSelected(it) },
                        onCancelEdit = {
                            isEditingMessage = false
                            messageToEditID = ""
                        },
                        changeCheckAndLaunchCameraToTrue = { checkAndLaunchCamera = true },
                        changeCheckAndLaunchGalleryToTrue = { checkAndLaunchGallery = true },
                        isRecording = state.isAudioRecording,
                        onStartAudioRecording = startAudioRecording,
                        onStopAudioRecording = stopAudioRecording,
                        onCancelAudioRecording = onCancelAudioRecording
                    )
                }
            }
        }
    }
}


@PreviewLightDark
@Composable
private fun ChatDetailScreenPreview() {
    val dummyMessages = listOf(
        Message(
            id = "1",
            content = "Hello, how are you?",
            role = Role.USER,
            image = null,
            timestamp = System.currentTimeMillis()
        ),
        Message(
            id = "2",
            content = "I'm fine, thank you!",
            role = Role.ASSISTANT,
            image = null,
            timestamp = System.currentTimeMillis()
        ),
        Message(
            id = "3",
            content = "Here's an image for you.",
            role = Role.USER,
            image = null,
            timestamp = System.currentTimeMillis()
        )
    )

    val dummyState = ChatState(
        chat = Chat(
            id = "1",
            title = "Chat Title",
            createdAt = System.currentTimeMillis(),
            messages = dummyMessages
        ),
        currentInput = "",
        selectedImageUri = null,
        isTyping = true,
        isSelectingImage = true,
        isTrendingLoading = false,
        isChatLoading = false,
        chatPreference = ChatPreference(
            mood = Mood.FRIENDLY,
            isReasoningEnabled = true,
            isOnlineSearchEnabled = true
        ),
        trendingTopics = listOf("Topic 1", "Topic 2", "Topic 3")
    )

    ChatDetailScreen(
        chatState = dummyState,
        onSaveTitleEdit = {},
        onTrendingTopicSelected = {},
        onInputChanged = {},
        onImageSelected = { _, _ -> },
        onRemoveImage = {},
        onToggleReasoning = {},
        onToggleOnlineSearch = {},
        onSendMessage = {},
        onTitleChanged = {},
        onMoodSelected = {},
        onToggleImageGeneration = {},
        onEditMessageClicked = {},
        onEditedMessageSent = {},
        startAudioRecording = { },
        stopAudioRecording = { },
        onCancelAudioRecording = {}
    )
}