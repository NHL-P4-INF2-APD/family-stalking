package com.familystalking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familystalking.app.presentation.forgotpassword.ForgotPasswordScreen
import com.familystalking.app.presentation.login.LoginScreen
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
                            // TODO: Implement HomeScreen
                        }
                    }
                }
            }
        }
    }
} 