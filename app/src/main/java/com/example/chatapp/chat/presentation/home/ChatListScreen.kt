package com.example.chatapp.chat.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.chat.domain.models.ChatSummary
import com.example.chatapp.chat.presentation.components.ChatSummaryItem
import com.example.chatapp.core.presentation.components.ShimmerListItem
import com.example.chatapp.core.presentation.components.SwipeableItemWithActions
import com.example.chatapp.ui.theme.ChatAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    state: ChatListState,
    onChatClicked: (String) -> Unit,
    onDeleteChatClicked: (String) -> Unit,
    onShareChatClicked: () -> Unit,
    onCreateNewChatClicked: () -> Unit
) {
    // Glowing animation for empty state
    val infiniteTransition = rememberInfiniteTransition()
    val glowingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val welcomeTextGlow by rememberInfiniteTransition().animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    var chatSummariesUi = remember {
        mutableStateListOf<ChatSummaryUi>()
    }

    LaunchedEffect(state.chats) {
        chatSummariesUi.clear()
        chatSummariesUi.addAll(
            state.chats.map {
                ChatSummaryUi(
                    id = it.id,
                    title = it.title,
                    updatedAt = it.updatedAt,
                    isOptionsVisible = false,
                    isDeleting = false
                )
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.chats)) },
                actions = {
                    state.profilePictureUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(R.string.profile_photo),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_photo),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNewChatClicked) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_chat))
            }
        }
    ) { padding ->
        if (state.isChatListLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                repeat(10) {
                    ShimmerListItem(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        } else if (chatSummariesUi.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(bottom = 8.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = glowingAlpha),
                )
                val name =
                    if (state.name.isEmpty()) stringResource(R.string.there) else state.name
                Text(
                    text = stringResource(R.string.welcome_no_chats, name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = welcomeTextGlow),
                                MaterialTheme.colorScheme.secondary.copy(alpha = welcomeTextGlow)
                            )
                        )
                    ),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tap_plus_to_start),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                itemsIndexed(
                    items = chatSummariesUi,
                    key = { _, chatSummary -> chatSummary.id })
                { index, chatSummary ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SwipeableItemWithActions(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(MaterialTheme.shapes.medium),
                            isRevealed = chatSummary.isOptionsVisible,
                            onExpanded = {
                                chatSummariesUi[index] = chatSummary.copy(
                                    isOptionsVisible = true
                                )
                            },
                            onCollapsed = {
                                chatSummariesUi[index] = chatSummary.copy(
                                    isOptionsVisible = false
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        chatSummariesUi[index] = chatSummary.copy(
                                            isOptionsVisible = false
                                        )
                                        onShareChatClicked()
                                    },
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteChatClicked(chatSummary.id)
                                        chatSummariesUi[index] = chatSummary.copy(
                                            isOptionsVisible = false,
                                            isDeleting = state.isChatDeleting
                                        )
                                        chatSummariesUi.removeAt(index)
                                    },
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
                                ChatSummaryItem(
                                    chat = chatSummary,
                                    onClick = { onChatClicked(chatSummary.id) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

data class ChatSummaryUi(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val isOptionsVisible: Boolean,
    val isDeleting: Boolean
)

@PreviewLightDark
@Composable
fun ChatListScreenPreview() {
    val chat = ChatSummary(
        id = "1",
        title = "Chat Title",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat2 = ChatSummary(
        id = "2",
        title = "Chat Title 2",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat3 = ChatSummary(
        id = "3",
        title = "Chat Title 3",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat4 = ChatSummary(
        id = "4",
        title = "Chat Title 4",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat5 = ChatSummary(
        id = "5",
        title = "Chat Title 5",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat6 = ChatSummary(
        id = "6",
        title = "Chat Title 6",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat7 = ChatSummary(
        id = "7",
        title = "Chat Title 7",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat8 = ChatSummary(
        id = "8",
        title = "Chat Title 8",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat9 = ChatSummary(
        id = "9",
        title = "Chat Title 9",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val chat10 = ChatSummary(
        id = "10",
        title = "Chat Title 10",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val state = ChatListState(
        isChatListLoading = false,
        chats = listOf(chat, chat2, chat3, chat4, chat5, chat6, chat7, chat8, chat9, chat10),
        isChatDeleting = false,
        profilePictureUri = null
    )
    ChatAppTheme {
        ChatListScreen(
            state = state,
            onChatClicked = {},
            onDeleteChatClicked = {},
            onShareChatClicked = {},
            onCreateNewChatClicked = {},
        )
    }
}