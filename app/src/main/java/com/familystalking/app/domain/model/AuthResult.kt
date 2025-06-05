package com.familystalking.app.domain.model

/**
 * Represents the result of an authentication operation.
 * 
 * This sealed class provides a type-safe way to handle authentication outcomes,
 * allowing for explicit handling of both successful and failed authentication attempts.
 * Used throughout the authentication flow to communicate operation results between layers.
 */
sealed class AuthResult {
    /**
     * Indicates that the authentication operation completed successfully.
     * This result is returned when sign-in, sign-up, password reset, or sign-out operations succeed.
     */
    object Success : AuthResult()
    
    /**
     * Indicates that the authentication operation failed.
     * 
     * @property error The specific authentication error that occurred, providing detailed
     *                 information about the failure reason for appropriate error handling
     */
    data class Error(val error: AuthError) : AuthResult()
} 