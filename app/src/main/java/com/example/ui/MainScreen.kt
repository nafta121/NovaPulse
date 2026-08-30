package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ContentVisibilitySettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen

enum class AppNavScreen {
    DASHBOARD,
    SETTINGS,
    CONTENT_VISIBILITY
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable { mutableStateOf(AppNavScreen.DASHBOARD) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppNavScreen.DASHBOARD -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { currentScreen = AppNavScreen.SETTINGS },
                    onOpenSettings = { currentScreen = AppNavScreen.SETTINGS },
                    onOpenContentVisibility = { currentScreen = AppNavScreen.CONTENT_VISIBILITY }
                )
            }
            AppNavScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppNavScreen.DASHBOARD },
                    onOpenContentVisibility = { currentScreen = AppNavScreen.CONTENT_VISIBILITY }
                )
            }
            AppNavScreen.CONTENT_VISIBILITY -> {
                ContentVisibilitySettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppNavScreen.SETTINGS }
                )
            }
        }
    }
}



