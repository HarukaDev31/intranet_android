package com.probusiness.intranet.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.probusiness.intranet.data.repository.AuthRepository
import com.probusiness.intranet.ui.login.LoginScreen
import com.probusiness.intranet.ui.support.NewTicketScreen
import com.probusiness.intranet.ui.support.SupportListScreen
import com.probusiness.intranet.ui.support.TicketChatScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface AuthRepositoryEntryPoint {
    fun authRepository(): AuthRepository
}

@Composable
private fun rememberAuthRepository(): AuthRepository {
    val appContext = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(appContext, AuthRepositoryEntryPoint::class.java).authRepository()
    }
}

@Composable
fun IntranetNavGraph(pendingSolicitudId: Int?) {
    val navController = rememberNavController()
    val isLoggedIn by rememberAuthRepository().isLoggedIn.collectAsStateWithLifecycle()

    val startDestination = if (isLoggedIn) Routes.SUPPORT_LIST else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(280)) },
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.SUPPORT_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.SUPPORT_LIST) {
            SupportListScreen(
                onOpenTicket = { id -> navController.navigate(Routes.ticketChat(id)) },
                onNewTicket = { navController.navigate(Routes.NEW_TICKET) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.NEW_TICKET) {
            NewTicketScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Routes.ticketChat(id)) {
                        popUpTo(Routes.SUPPORT_LIST)
                    }
                },
            )
        }

        composable(
            route = Routes.TICKET_CHAT,
            arguments = listOf(navArgument(Routes.TICKET_CHAT_ARG) { type = NavType.IntType }),
        ) {
            TicketChatScreen(onBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(pendingSolicitudId, isLoggedIn) {
        if (pendingSolicitudId != null && isLoggedIn) {
            navController.navigate(Routes.ticketChat(pendingSolicitudId))
        }
    }
}
