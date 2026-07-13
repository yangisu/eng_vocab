package com.yangi.engvocab.navigation

import com.yangi.engvocab.core.repository.WordFilter

sealed interface AppDestination {
    val route: String

    data object Home : AppDestination {
        override val route = "home"
    }

    data object Books : AppDestination {
        override val route = "books"
    }

    data object BookDetail : AppDestination {
        const val ARG_BOOK_ID = "bookId"
        override val route = "books/{$ARG_BOOK_ID}"
        fun createRoute(bookId: Long) = "books/$bookId"
    }

    data object Review : AppDestination {
        override val route = "review"
    }

    data object Collection : AppDestination {
        const val ARG_FILTER = "filter"
        override val route = "collection/{${ARG_FILTER}}"
        fun createRoute(filter: WordFilter) = "collection/${filter.name}"
    }


    data object Settings : AppDestination {
        override val route = "settings"
    }

    data object ImportPhoto : AppDestination {
        const val ARG_BOOK_ID = "bookId"
        override val route = "import-photo?$ARG_BOOK_ID={$ARG_BOOK_ID}"
        fun createRoute(bookId: Long? = null) = "import-photo?$ARG_BOOK_ID=${bookId ?: -1L}"
    }

    data object Study : AppDestination {
        const val ARG_BOOK_ID = "bookId"
        const val ARG_DUE = "due"
        const val ARG_IDS = "ids"
        override val route = "study?$ARG_BOOK_ID={$ARG_BOOK_ID}&$ARG_DUE={$ARG_DUE}&$ARG_IDS={$ARG_IDS}"
        fun createBookRoute(bookId: Long) = "study?$ARG_BOOK_ID=$bookId&$ARG_DUE=false&$ARG_IDS="
        fun createDueRoute(bookId: Long? = null) = "study?$ARG_BOOK_ID=${bookId ?: -1L}&$ARG_DUE=true&$ARG_IDS="
        fun createIdsRoute(ids: List<Long>) = "study?$ARG_BOOK_ID=-1&$ARG_DUE=false&$ARG_IDS=${ids.joinToString(",")}"
    }
}

