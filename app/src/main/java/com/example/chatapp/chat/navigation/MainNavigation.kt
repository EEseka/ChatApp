package com.example.chatapp.chat.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.chatapp.chat.presentation.home.HomeScreen
import com.example.chatapp.chat.presentation.settings.SettingsEvents
import com.example.chatapp.chat.presentation.settings.SettingsScreen
import com.example.chatapp.chat.presentation.settings.SettingsViewModel
import com.example.chatapp.core.presentation.util.ObserveAsEvents
import com.example.chatapp.core.presentation.util.toString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    mainEventBus: MainEventBus = koinInject(),
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val user = settingsViewModel.user

    ObserveAsEvents(events = mainEventBus.events) { event ->
        when (event) {
            MainEvent.EmailUpdated -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.email_updated_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is MainEvent.Error -> {
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

    val screens = listOf(MainNavDestinations.Home, MainNavDestinations.Settings)
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
            startDestination = MainNavDestinations.Home.route
        ) {
            composable(MainNavDestinations.Home.route) {
                HomeScreen(
                    displayName = user?.displayName!!,
                    photoUrl = user?.photoUrl
                )
            }

            composable(MainNavDestinations.Settings.route) {
                SettingsScreen(
                    state = settingsState,
                    currentPhotoUri = user?.photoUrl,
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
                )
            }
            // Other composable destinations like Chat, Profile, Settings
        }
    }
}
