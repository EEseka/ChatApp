package com.example.chatapp.chat.navigation

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chatapp.R
import com.example.chatapp.chat.presentation.MainEvent
import com.example.chatapp.chat.presentation.MainEventBus
import com.example.chatapp.chat.presentation.home.ChatDetailScreen
import com.example.chatapp.chat.presentation.home.ChatEvents
import com.example.chatapp.chat.presentation.home.ChatListScreen
import com.example.chatapp.chat.presentation.home.ChatViewModel
import com.example.chatapp.chat.presentation.settings.SettingsEvents
import com.example.chatapp.chat.presentation.settings.SettingsScreen
import com.example.chatapp.chat.presentation.settings.SettingsViewModel
import com.example.chatapp.core.presentation.util.ObserveAsEvents
import com.example.chatapp.core.presentation.util.toString
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainEventBus: MainEventBus = koinInject(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(events = mainEventBus.events) { event ->
        when (event) {
            MainEvent.EmailUpdated -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.email_updated_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is MainEvent.AuthError -> {
                Toast.makeText(
                    context,
                    event.error.toString(context),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is MainEvent.DatabaseError -> {
                Toast.makeText(
                    context,
                    event.error.toString(context),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is MainEvent.NetworkingError -> {
                Toast.makeText(
                    context,
                    event.error.toString(context),
                    Toast.LENGTH_SHORT
                ).show()
            }

            MainEvent.ProfileUpdateComplete -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_updated_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }

            MainEvent.AccountDeletionComplete -> {
                onLogout()
            }

            MainEvent.SignOutComplete -> {
                onLogout()
            }
        }
    }

    val screens = listOf(MainNavDestinations.Chat, MainNavDestinations.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            screens.forEach { screen ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                item(
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) screen.filledIcon!! else screen.outlinedIcon!!,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(screen.label)) }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = MainNavDestinations.Chat.route
        ) {
            composable(MainNavDestinations.Chat.route) {
                val chatViewModel = koinViewModel<ChatViewModel>()
                val chatState by chatViewModel.state.collectAsStateWithLifecycle()
                val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
                NavigableListDetailPaneScaffold(
                    navigator = navigator,
                    listPane = {
                        AnimatedPane {
                            ChatListScreen(
                                state = chatState,
                                onChatClicked = { chatId ->
                                    chatViewModel.onEvent(ChatEvents.OnChatSelected(chatId))
                                    scope.launch {
                                        navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail)
                                    }
                                },
                                onDeleteChatClicked = { chatId ->
                                    chatViewModel.onEvent(ChatEvents.OnDeleteChat(chatId))
                                },
                                onShareChatClicked = {},
                                onCreateNewChatClicked = {
                                    chatViewModel.onEvent(ChatEvents.OnCreateNewChat)
                                    scope.launch {
                                        navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail)
                                    }
                                },
                            )
                        }
                    },
                    detailPane = {
                        AnimatedPane {
                            ChatDetailScreen(
                                chatState = chatState.selectedChat,
                                onSaveTitleEdit = { chatViewModel.onEvent(ChatEvents.OnSaveTitleEdit) },
                                onTitleChanged = {
                                    chatViewModel.onEvent(ChatEvents.OnChatTitleChanged(it))
                                },
                                onTrendingTopicSelected = {
                                    chatViewModel.onEvent(ChatEvents.OnTrendingTopicSelected(it))
                                },
                                onInputChanged = {
                                    chatViewModel.onEvent(ChatEvents.OnInputChanged(it))
                                },
                                onImageSelected = { uri, extension ->
                                    chatViewModel.onEvent(
                                        ChatEvents.OnImageSelected(uri, extension)
                                    )
                                },
                                onRemoveImage = {
                                    chatViewModel.onEvent(ChatEvents.OnRemoveImage)
                                },
                                onToggleReasoning = {
                                    chatViewModel.onEvent(ChatEvents.OnToggleReasoning)
                                },
                                onToggleOnlineSearch = {
                                    chatViewModel.onEvent(ChatEvents.OnToggleOnlineSearch)
                                },
                                onToggleImageGeneration = {
                                    chatViewModel.onEvent(ChatEvents.OnToggleImageGeneration)
                                },
                                onMoodSelected = {
                                    chatViewModel.onEvent(ChatEvents.OnMoodSelected(it))
                                },
                                onSendMessage = {
                                    chatViewModel.onEvent(ChatEvents.OnMessageSent)
                                },
                                onEditMessageClicked = {
                                    chatViewModel.onEvent(ChatEvents.OnEditMessage(it))
                                },
                                onEditedMessageSent = {
                                    chatViewModel.onEvent(ChatEvents.OnEditedMessageSent(it))
                                },
                                startAudioRecording = {
                                    chatViewModel.onEvent(ChatEvents.OnStartAudioRecording)
                                },
                                stopAudioRecording = {
                                    chatViewModel.onEvent(ChatEvents.OnStopAudioRecording)
                                },
                                onCancelAudioRecording = {
                                    chatViewModel.onEvent(ChatEvents.OnCancelAudioRecording)
                                }
                            )
                        }
                    }
                )
            }

            composable(MainNavDestinations.Settings.route) {
                val settingsViewModel = koinViewModel<SettingsViewModel>()
                val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

                SettingsScreen(
                    state = settingsState,
                    onDisplayNameChanged = {
                        settingsViewModel.onEvent(SettingsEvents.OnDisplayNameChanged(it))
                    },
                    onUpdateProfileClicked = {
                        settingsViewModel.onEvent(SettingsEvents.OnUpdateProfileClicked)
                    },
                    onDeleteAccountClicked = {
                        settingsViewModel.onEvent(SettingsEvents.OnDeleteAccountClicked)
                    },
                    onSignOutClicked = {
                        settingsViewModel.onEvent(SettingsEvents.OnSignOutClicked)
                    },
                    onPhotoSelected = { uri, extension ->
                        settingsViewModel.onEvent(SettingsEvents.OnPhotoSelected(uri, extension))
                    },
                    onScreenLeave = {
                        settingsViewModel.onEvent(SettingsEvents.OnScreenLeave)
                    },
                    onScreenReturn = {
                        settingsViewModel.onEvent(SettingsEvents.OnScreenReturn)
                    },
                    onSetActivityContext = {
                        settingsViewModel.onEvent(SettingsEvents.OnSetActivityContext(it))
                    },
                    onClearActivityContext = {
                        settingsViewModel.onEvent(SettingsEvents.OnClearActivityContext)
                    },
                )
            }
        }
    }
}
