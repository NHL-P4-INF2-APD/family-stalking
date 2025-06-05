package com.familystalking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.presentation.MainViewModel
import com.familystalking.app.presentation.forgotpassword.ForgotPasswordScreen
import com.familystalking.app.presentation.home.HomeScreen
import com.familystalking.app.presentation.login.LoginScreen
import com.familystalking.app.presentation.map.MapScreen
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.settings.SettingsScreen
import com.familystalking.app.presentation.signup.SignupScreen
import com.familystalking.app.presentation.family.FamilyScreen
import com.familystalking.app.presentation.family.FamilyQrScreen
import com.familystalking.app.presentation.family.CameraScreen
import com.familystalking.app.ui.theme.FamilyStalkingTheme
import com.familystalking.app.ui.theme.PrimaryGreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.familystalking.app.presentation.agenda.AddEventScreen // Import AddEventScreen
import com.familystalking.app.presentation.agenda.AgendaScreen
import com.familystalking.app.presentation.family.FamilyViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyStalkingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = hiltViewModel()
                    val sessionState by viewModel.sessionState.collectAsState()
                    val familyViewModel: FamilyViewModel = hiltViewModel()
                    val state by familyViewModel.state.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.checkSession()
                    }

                    LaunchedEffect(sessionState) {
                        when (sessionState) {
                            SessionState.Authenticated -> {
                                val mainAuthenticatedRoutes = listOf(
                                    Screen.Map.route, Screen.Agenda.route, Screen.AddEvent.route,
                                    Screen.Family.route, Screen.Settings.route
                                )
                                if (navController.currentDestination?.route !in mainAuthenticatedRoutes &&
                                    navController.currentBackStack.value.none { entry -> entry.destination.route in mainAuthenticatedRoutes }) {
                                    navController.navigate(Screen.Map.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            }
                            SessionState.Unauthenticated -> {
                                val authRoutes = listOf(Screen.Login.route, Screen.Signup.route, Screen.ForgotPassword.route)
                                if (navController.currentDestination?.route !in authRoutes) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            SessionState.Loading -> { }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route
                    ) {
                        composable(Screen.Login.route) { LoginScreen(navController) }
                        composable(Screen.Signup.route) { SignupScreen(navController) }
                        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.Map.route) { MapScreen(navController) }
                        composable(Screen.Agenda.route) { AgendaScreen(navController = navController) }
                        composable(Screen.AddEvent.route) { AddEventScreen(navController = navController) } // Added route
                        composable(Screen.Family.route) { FamilyScreen(navController) }
                        composable(Screen.FamilyQr.route) { FamilyQrScreen(navController) }
                        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
                        composable(Screen.Camera.route) { CameraScreen(navController = navController) }
                    }

                    state.pendingRequests.takeIf { it.isNotEmpty() }?.firstOrNull()?.let { request ->
                        AlertDialog(
                            onDismissRequest = { familyViewModel.dismissRequestDialog() },
                            title = { Text("Friend Request") },
                            text = { Text("${request.senderId ?: request.senderId} wants to add you to their family.") },
                            confirmButton = {
                                Button(
                                    onClick = { familyViewModel.acceptFriendshipRequest(request.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) { Text("Accept") }
                            },
                            dismissButton = {
                                TextButton(onClick = { familyViewModel.rejectFriendshipRequest(request.id) }) { Text("Reject") }
                            }
                        )
                    }

                    state.error?.let { error ->
                        LaunchedEffect(error) {
                            // snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
                            // familyViewModel.clearError()
                        }
                    }
                }
            }
        }
    }
}