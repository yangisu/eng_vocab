package com.yangi.engvocab.navigation

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

    data object Settings : AppDestination {
        override val route = "settings"
    }

    data object ImportPhoto : AppDestination {
        override val route = "import-photo"
    }

    data object Study : AppDestination {
        override val route = "study"
    }
}

