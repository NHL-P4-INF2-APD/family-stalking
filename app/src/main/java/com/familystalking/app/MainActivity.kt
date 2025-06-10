package com.familystalking.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.presentation.MainViewModel
import com.familystalking.app.presentation.agenda.AddEventScreen
import com.familystalking.app.presentation.agenda.AgendaScreen
import com.familystalking.app.presentation.family.CameraScreen
import com.familystalking.app.presentation.family.FamilyQrScreen
import com.familystalking.app.presentation.family.FamilyScreen
import com.familystalking.app.presentation.family.FamilyViewModel
import com.familystalking.app.presentation.forgotpassword.ForgotPasswordScreen
import com.familystalking.app.presentation.home.HomeScreen
import com.familystalking.app.presentation.login.LoginScreen
import com.familystalking.app.presentation.map.MapScreen
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.settings.SettingsScreen
import com.familystalking.app.presentation.signup.SignupScreen
import com.familystalking.app.ui.theme.FamilyStalkingTheme
import com.familystalking.app.ui.theme.PrimaryGreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
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

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // This LaunchedEffect handles navigation based on session state (login/logout)
                    LaunchedEffect(sessionState) {
                        when (sessionState) {
                            SessionState.Unauthenticated -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            SessionState.Authenticated -> {
                                // Navigate to map screen after successful login
                                navController.navigate(Screen.Map.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route,
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
                    }

                    // Global error snackbar
                    familyState.error?.let { error ->
                        LaunchedEffect(error) {
                            snackbarHostState.showSnackbar(
                                message = error,
                                duration = SnackbarDuration.Short
                            )
                            familyViewModel.clearError()
                        }
                    }

                    // Global success snackbar
                    familyState.successMessage?.let { message ->
                        LaunchedEffect(message) {
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                            familyViewModel.clearSuccessMessage()
                        }
                    }

                    // This shows the friend request dialog globally, on top of any screen
                    familyState.pendingRequests.firstOrNull()?.let { request ->
                        // The AlertDialog is now handled in the FamilyScreen for better context
                    }
                }
            }
        }
    }
}