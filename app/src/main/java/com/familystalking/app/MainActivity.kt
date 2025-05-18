package com.familystalking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.familystalking.app.presentation.signup.SignupScreen
import com.familystalking.app.ui.theme.FamilyStalkingTheme
import dagger.hilt.android.AndroidEntryPoint

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
                        // Add other routes for the bottom navigation
                        composable(Screen.Agenda.route) {
                            // Placeholder for Agenda screen
                            HomeScreen(navController) // Temporarily using HomeScreen
                        }
                        composable(Screen.Family.route) {
                            // Placeholder for Family screen
                            HomeScreen(navController) // Temporarily using HomeScreen
                        }
                        composable(Screen.Settings.route) {
                            // Placeholder for Settings screen
                            HomeScreen(navController) // Temporarily using HomeScreen
                        }
                    }
                }
            }
        }
    }
}
