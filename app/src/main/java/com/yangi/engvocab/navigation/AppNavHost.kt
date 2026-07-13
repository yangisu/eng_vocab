package com.yangi.engvocab.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Home.route) { DestinationTitle("오늘의 학습") }
        composable(AppDestination.Books.route) { DestinationTitle("내 단어장") }
        composable(AppDestination.Review.route) { DestinationTitle("복습") }
        composable(AppDestination.Settings.route) { DestinationTitle("설정") }
        composable(AppDestination.ImportPhoto.route) { DestinationTitle("사진으로 만들기") }
        composable(AppDestination.Study.route) { DestinationTitle("학습") }
    }
}

@Composable
private fun DestinationTitle(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title)
    }
}

