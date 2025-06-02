package com.familystalking.app.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Map : Screen("map")
    object Agenda : Screen("agenda")
    object Family : Screen("family")
    object Settings : Screen("settings")
    object FamilyQr : Screen("family/qr")
    object Camera : Screen("camera")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when (route) {
                Login.route -> Login
                Signup.route -> Signup
                ForgotPassword.route -> ForgotPassword
                Home.route -> Home
                Map.route -> Map
                Agenda.route -> Agenda
                Family.route -> Family
                Settings.route -> Settings
                FamilyQr.route -> FamilyQr
                Camera.route -> Camera
                else -> Login
            }
        }
    }
}
