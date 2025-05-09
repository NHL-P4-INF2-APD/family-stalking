package com.familystalking.app.domain.model

sealed class AuthError(val message: String) {
    object InvalidEmail : AuthError("Invalid email address")
    object WeakPassword : AuthError("Password must be at least 6 characters")
    object PasswordsDoNotMatch : AuthError("Passwords do not match")
    object EmailAlreadyInUse : AuthError("Email is already in use")
    object InvalidCredentials : AuthError("Invalid email or password")
    object NetworkError : AuthError("Network error occurred")
    object UnknownError : AuthError("An unknown error occurred")
} 