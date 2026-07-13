package com.yangi.engvocab

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yangi.engvocab.navigation.AppDestination
import com.yangi.engvocab.navigation.AppNavHost

private data class BottomDestination(
    val destination: AppDestination,
    val label: String,
    val marker: String,
)

private val bottomDestinations = listOf(
    BottomDestination(AppDestination.Home, "홈", "H"),
    BottomDestination(AppDestination.Books, "단어장", "B"),
    BottomDestination(AppDestination.Review, "복습", "R"),
    BottomDestination(AppDestination.Settings, "설정", "S"),
)

@Composable
fun EngVocabApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(AppDestination.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(item.marker) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

