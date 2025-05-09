package com.familystalking.app.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when (route) {
                Login.route -> Login
                Signup.route -> Signup
                ForgotPassword.route -> ForgotPassword
                Home.route -> Home
                else -> Login
            }
        }
    }
} 