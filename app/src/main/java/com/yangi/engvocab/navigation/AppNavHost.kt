package com.yangi.engvocab.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.yangi.engvocab.AppContainer
import com.yangi.engvocab.feature.books.BookDetailRoute
import com.yangi.engvocab.feature.books.BookDetailViewModel
import com.yangi.engvocab.feature.books.BookListRoute
import com.yangi.engvocab.feature.books.BookListViewModel
import com.yangi.engvocab.feature.books.FilteredWordsRoute
import com.yangi.engvocab.feature.books.FilteredWordsViewModel
import com.yangi.engvocab.feature.home.HomeRoute
import com.yangi.engvocab.feature.home.HomeViewModel
import com.yangi.engvocab.feature.review.ReviewRoute
import com.yangi.engvocab.feature.review.ReviewViewModel
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.feature.importphoto.PhotoImportRoute
import com.yangi.engvocab.feature.importphoto.PhotoImportViewModel
import com.yangi.engvocab.feature.settings.SettingsRoute
import com.yangi.engvocab.feature.study.StudyEntryRoute
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
        composable(AppDestination.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(container.vocabularyRepository, container.clock),
            )
            HomeRoute(
                viewModel = homeViewModel,
                onStartDue = { navController.navigate(AppDestination.Study.createDueRoute()) },
                onOpenBook = { navController.navigate(AppDestination.BookDetail.createRoute(it)) },
                onPhotoImport = { navController.navigate(AppDestination.ImportPhoto.createRoute()) },
                onBooks = { navController.navigate(AppDestination.Books.route) },
                onImportant = { navController.navigate(AppDestination.Collection.createRoute(WordFilter.IMPORTANT)) },
                onWrong = { navController.navigate(AppDestination.Collection.createRoute(WordFilter.WRONG)) },
            )
        }
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
                onPhotoAdd = { navController.navigate(AppDestination.ImportPhoto.createRoute(it)) },
                onStudy = { navController.navigate(AppDestination.Study.createBookRoute(it)) },
            )
        }
        composable(AppDestination.Review.route) {
            val reviewViewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModel.Factory(container.vocabularyRepository, container.clock),
            )
            ReviewRoute(
                viewModel = reviewViewModel,
                onStartAll = { navController.navigate(AppDestination.Study.createDueRoute()) },
                onStartBook = { navController.navigate(AppDestination.Study.createDueRoute(it)) },
            )
        }
        composable(
            route = AppDestination.Collection.route,
            arguments = listOf(
                navArgument(AppDestination.Collection.ARG_FILTER) { type = NavType.StringType },
            ),
        ) { entry ->
            val filter = runCatching {
                WordFilter.valueOf(requireNotNull(entry.arguments?.getString(AppDestination.Collection.ARG_FILTER)))
            }.getOrDefault(WordFilter.IMPORTANT)
            val filteredViewModel: FilteredWordsViewModel = viewModel(
                key = "collection-$filter",
                factory = FilteredWordsViewModel.Factory(container.vocabularyRepository, filter),
            )
            FilteredWordsRoute(
                viewModel = filteredViewModel,
                onBack = navController::popBackStack,
                onStudy = { navController.navigate(AppDestination.Study.createIdsRoute(it)) },
            )
        }
        composable(AppDestination.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(container.apiKeyStore),
            )
            SettingsRoute(settingsViewModel, onBack = navController::popBackStack)
        }
        composable(
            route = AppDestination.ImportPhoto.route,
            arguments = listOf(
                navArgument(AppDestination.ImportPhoto.ARG_BOOK_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val initialBookId = entry.arguments?.getLong(AppDestination.ImportPhoto.ARG_BOOK_ID) ?: -1L
            val importViewModel: PhotoImportViewModel = viewModel(
                factory = PhotoImportViewModel.Factory(
                    container.vocabularyRepository,
                    container.openAiService,
                    container.imagePreprocessor,
                    container.tempImageStore,
                    SavedStateHandle(if (initialBookId > 0) mapOf("photo_import_book_id" to initialBookId) else emptyMap()),
                ),
            )
            PhotoImportRoute(importViewModel, container.tempImageStore, navController::popBackStack, { navController.navigate(AppDestination.BookDetail.createRoute(it)) }, { navController.navigate(AppDestination.Study.createIdsRoute(it)) })
        }
        composable(
            route = AppDestination.Study.route,
            arguments = listOf(
                navArgument(AppDestination.Study.ARG_BOOK_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(AppDestination.Study.ARG_DUE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(AppDestination.Study.ARG_IDS) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val bookId = entry.arguments?.getLong(AppDestination.Study.ARG_BOOK_ID)?.takeIf { it > 0 }
            val due = entry.arguments?.getBoolean(AppDestination.Study.ARG_DUE) ?: false
            val ids = entry.arguments?.getString(AppDestination.Study.ARG_IDS)
                .orEmpty().split(',').mapNotNull(String::toLongOrNull)
            StudyEntryRoute(
                repository = container.vocabularyRepository,
                clock = container.clock,
                bookId = bookId,
                dueOnly = due,
                explicitIds = ids,
                onRetryWrong = { navController.navigate(AppDestination.Study.createIdsRoute(it)) },
                onDone = navController::popBackStack,
            )
        }
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

