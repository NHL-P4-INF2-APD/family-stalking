package com.familystalking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.presentation.MainViewModel
import com.familystalking.app.presentation.agenda.AddEventScreen
import com.familystalking.app.presentation.agenda.AgendaScreen
import com.familystalking.app.presentation.family.CameraScreen
import com.familystalking.app.presentation.family.FamilyQrScreen
import com.familystalking.app.presentation.family.FamilyScreen
import com.familystalking.app.presentation.family.FamilyViewModel
import com.familystalking.app.presentation.family.PendingRequestsScreen
import com.familystalking.app.presentation.forgotpassword.ForgotPasswordScreen
import com.familystalking.app.presentation.login.LoginScreen
import com.familystalking.app.presentation.map.MapScreen
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.settings.SettingsScreen
import com.familystalking.app.presentation.signup.SignupScreen
import com.familystalking.app.ui.theme.FamilyStalkingTheme
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyStalkingTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = hiltViewModel()
                val familyViewModel: FamilyViewModel = hiltViewModel()
                val sessionState by mainViewModel.sessionState.collectAsState()
                val familyState by familyViewModel.state.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                var determinedStartDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(sessionState) {
                    if (determinedStartDestination == null) {
                        when (sessionState) {
                            is SessionState.Authenticated -> {
                                Log.d("MainActivity", "Initial state determined: Authenticated. Setting start destination to Map.")
                                determinedStartDestination = Screen.Map.route
                            }
                            is SessionState.Unauthenticated -> {
                                Log.d("MainActivity", "Initial state determined: Unauthenticated. Setting start destination to Login.")
                                determinedStartDestination = Screen.Login.route
                            }
                            is SessionState.Loading -> {
                                Log.d("MainActivity", "Initial state: Loading session...")
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (determinedStartDestination != null) {
                        AppNavHost(
                            navController = navController,
                            startDestination = determinedStartDestination!!,
                            sessionState = sessionState,
                            familyViewModel = familyViewModel
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                            Log.d("MainActivity", "Showing loading indicator: startDestination is null.")
                        }
                    }

                    familyState.error?.let { error ->
                        LaunchedEffect(error) {
                            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
                            familyViewModel.clearError()
                        }
                    }
                    familyState.successMessage?.let { message ->
                        LaunchedEffect(message) {
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                            familyViewModel.clearSuccessMessage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    sessionState: SessionState,
    familyViewModel: FamilyViewModel
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route

    var hasPerformedInitialNavigation by remember(startDestination) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPerformedInitialNavigation && startDestination.isNotEmpty()) {
            Log.d("AppNavHost", "Initial navigation to startDestination: $startDestination")
            navController.navigate(startDestination) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            hasPerformedInitialNavigation = true
        }
    }

    LaunchedEffect(sessionState, currentRoute, hasPerformedInitialNavigation) {
        if (hasPerformedInitialNavigation) {
            val isAuthenticatedScreen = currentRoute != Screen.Login.route &&
                    currentRoute != Screen.Signup.route &&
                    currentRoute != Screen.ForgotPassword.route
            // Add any other non-authenticated routes here

            if (sessionState is SessionState.Unauthenticated && isAuthenticatedScreen) {
                Log.d("AppNavHost", "Session Unauthenticated while on an authenticated screen ($currentRoute). Navigating to Login.")
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Signup.route) { SignupScreen(navController) }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }
        composable(Screen.Map.route) { MapScreen(navController = navController) }
        composable(Screen.Family.route) { FamilyScreen(navController, familyViewModel) }
        composable(Screen.FamilyQr.route) { FamilyQrScreen(navController, familyViewModel) }
        composable(Screen.Camera.route) { CameraScreen(navController, familyViewModel) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.Agenda.route) { AgendaScreen(navController = navController) }
        composable(Screen.AddEvent.route) { AddEventScreen(navController = navController) }
        composable(Screen.PendingRequests.route) { PendingRequestsScreen(navController, familyViewModel) }
    }
}