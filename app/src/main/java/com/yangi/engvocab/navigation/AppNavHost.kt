package com.yangi.engvocab.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.yangi.engvocab.AppContainer
import com.yangi.engvocab.feature.books.BookDetailRoute
import com.yangi.engvocab.feature.books.BookDetailViewModel
import com.yangi.engvocab.feature.books.BookListRoute
import com.yangi.engvocab.feature.books.BookListViewModel
import com.yangi.engvocab.feature.settings.SettingsRoute
import com.yangi.engvocab.feature.settings.SettingsViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Home.route) { DestinationTitle("오늘의 학습") }
        composable(AppDestination.Books.route) {
            val booksViewModel: BookListViewModel = viewModel(
                factory = BookListViewModel.Factory(container.vocabularyRepository),
            )
            BookListRoute(
                viewModel = booksViewModel,
                onOpenBook = { navController.navigate(AppDestination.BookDetail.createRoute(it)) },
            )
        }
        composable(
            route = AppDestination.BookDetail.route,
            arguments = listOf(
                navArgument(AppDestination.BookDetail.ARG_BOOK_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val bookId = requireNotNull(entry.arguments?.getLong(AppDestination.BookDetail.ARG_BOOK_ID))
            val detailViewModel: BookDetailViewModel = viewModel(
                key = "book-$bookId",
                factory = BookDetailViewModel.Factory(
                    container.vocabularyRepository,
                    container.openAiService,
                    bookId,
                ),
            )
            BookDetailRoute(
                viewModel = detailViewModel,
                onBack = navController::popBackStack,
                onPhotoAdd = { navController.navigate(AppDestination.ImportPhoto.route) },
                onStudy = { navController.navigate(AppDestination.Study.route) },
            )
        }
        composable(AppDestination.Review.route) { DestinationTitle("복습") }
        composable(AppDestination.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(container.apiKeyStore),
            )
            SettingsRoute(settingsViewModel, onBack = navController::popBackStack)
        }
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

