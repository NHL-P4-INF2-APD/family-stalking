package com.familystalking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.familystalking.app.presentation.family.FamilyViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyStalkingTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = hiltViewModel()
                val sessionState by viewModel.sessionState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val familyViewModel: FamilyViewModel = hiltViewModel()
                val state by familyViewModel.state.collectAsState()

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = 80.dp)
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LaunchedEffect(Unit) {
                            viewModel.checkSession()
                        }

                        LaunchedEffect(sessionState) {
                            when (sessionState) {
                                SessionState.Authenticated -> {
                                    navController.navigate(Screen.Map.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                                SessionState.Unauthenticated -> {
                                    if (navController.currentDestination?.route != Screen.Login.route) {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                                SessionState.Loading -> { /* Do nothing while loading */ }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = Screen.Login.route
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(navController)
                            }
                            composable(Screen.Signup.route) {
                                SignupScreen(navController)
                            }
                            composable(Screen.ForgotPassword.route) {
                                ForgotPasswordScreen(navController)
                            }
                            composable(Screen.Home.route) {
                                HomeScreen(navController)
                            }
                            composable(Screen.Map.route) {
                                MapScreen(navController)
                            }
                            composable(Screen.Agenda.route) {

                                HomeScreen(navController)
                            }
                            composable(Screen.Family.route) {
                                FamilyScreen(navController)
                            }
                            composable(Screen.FamilyQr.route) {
                                FamilyQrScreen(navController)
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(navController)
                            }
                            composable(Screen.Camera.route) {
                                CameraScreen(navController = navController)
                            }
                        }

                        // Global friendship request dialog
                        state.pendingRequests.firstOrNull()?.let { request ->
                            AlertDialog(
                                onDismissRequest = { familyViewModel.dismissRequestDialog() },
                                title = { Text("Friend Request") },
                                text = { Text("${request.senderId} wants to add you to their family") },
                                confirmButton = {
                                    Button(
                                        onClick = { familyViewModel.acceptFriendshipRequest(request.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                    ) {
                                        Text("Accept")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { familyViewModel.rejectFriendshipRequest(request.id) }) {
                                        Text("Reject")
                                    }
                                }
                            )
                        }

                        // Global error snackbar
                        state.error?.let { error ->
                            LaunchedEffect(error) {
                                snackbarHostState.showSnackbar(
                                    message = error,
                                    duration = SnackbarDuration.Short
                                )
                                familyViewModel.clearError()
                            }
                        }

                        // Global success snackbar
                        state.successMessage?.let { message ->
                            LaunchedEffect(message) {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                                familyViewModel.clearSuccessMessage()
                            }
                        }

                        // Friend Added Dialog
                        if (state.showFriendAddedDialog) {
                            AlertDialog(
                                onDismissRequest = { familyViewModel.dismissFriendAddedDialog() },
                                title = { Text("Friend Added!") },
                                text = { Text("You and ${state.newlyAddedFriend?.name ?: "your new friend"} are now friends.") },
                                confirmButton = {
                                    Button(onClick = { familyViewModel.dismissFriendAddedDialog() }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}