package com.familystalking.app.domain.model

/**
 * Represents various authentication error types that can occur during user authentication operations.
 * 
 * This sealed class encapsulates all possible authentication errors with descriptive error messages
 * to provide clear feedback to users and facilitate error handling throughout the application.
 * 
 * @property message The human-readable error message associated with this error type
 */
sealed class AuthError(val message: String) {
    /**
     * Indicates that the provided email address format is invalid.
     * Typically thrown during validation of email input fields.
     */
    object InvalidEmail : AuthError("Invalid email address")
    
    /**
     * Indicates that the provided password does not meet minimum security requirements.
     * Currently enforces a minimum length of 6 characters.
     */
    object WeakPassword : AuthError("Password must be at least 6 characters")
    
    /**
     * Indicates that password and password confirmation fields do not match.
     * Used during user registration to ensure password accuracy.
     */
    object PasswordsDoNotMatch : AuthError("Passwords do not match")
    
    /**
     * Indicates that the email address is already registered to another user account.
     * Occurs during user registration when attempting to use an existing email.
     */
    object EmailAlreadyInUse : AuthError("Email is already in use")
    
    /**
     * Indicates that the provided email and password combination is incorrect.
     * Used during sign-in operations when credentials cannot be verified.
     */
    object InvalidCredentials : AuthError("Invalid email or password")
    
    /**
     * Indicates that a network-related error occurred during authentication.
     * This includes connectivity issues, timeouts, or server unavailability.
     */
    object NetworkError : AuthError("Network error occurred")
    
    /**
     * Represents any unexpected error that doesn't fall into other categories.
     * Used as a fallback for unhandled exceptions during authentication operations.
     */
    object UnknownError : AuthError("An unknown error occurred")
} 