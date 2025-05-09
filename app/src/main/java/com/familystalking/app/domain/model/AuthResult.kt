package com.familystalking.app.domain.model

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val error: AuthError) : AuthResult()
} 