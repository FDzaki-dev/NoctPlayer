package com.noctplayer.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noctplayer.app.data.repository.MediaRepository
import com.noctplayer.app.ui.library.LibraryScreen
import com.noctplayer.app.ui.library.LibraryViewModel
import com.noctplayer.app.ui.player.PlayerScreen
import com.noctplayer.app.ui.player.PlayerViewModel

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player/{mediaId}"
    fun playerRoute(mediaId: Long) = "player/$mediaId"
}

@Composable
fun NoctNavGraph(repository: MediaRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            val vm: LibraryViewModel = viewModel(factory = viewModelFactory { LibraryViewModel(repository) })
            LibraryScreen(
                viewModel = vm,
                onOpenVideo = { id -> navController.navigate(Routes.playerRoute(id)) }
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: -1L
            val vm: PlayerViewModel = viewModel(factory = viewModelFactory { PlayerViewModel(repository) })
            PlayerScreen(
                viewModel = vm,
                mediaStoreId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun <T : androidx.lifecycle.ViewModel> viewModelFactory(create: () -> T) =
    object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <U : androidx.lifecycle.ViewModel> create(modelClass: Class<U>): U = create() as U
    }
