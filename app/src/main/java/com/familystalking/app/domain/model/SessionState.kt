package com.familystalking.app.domain.model

/**
 * Represents the current authentication state of the user session.
 * 
 * This sealed class provides a type-safe way to track and respond to changes in user
 * authentication status throughout the application lifecycle. It enables reactive UI
 * updates and proper navigation flow based on the user's current session state.
 */
sealed class SessionState {
    /**
     * Indicates that the authentication state is currently being determined.
     * This state is typically used during app initialization while checking for
     * existing user sessions or during authentication operations.
     */
    data object Loading : SessionState()
    
    /**
     * Indicates that the user is successfully authenticated and has a valid session.
     * In this state, the user has access to all protected features of the application.
     */
    data object Authenticated : SessionState()
    
    /**
     * Indicates that the user is not authenticated or their session has expired.
     * In this state, the user should be redirected to authentication screens and
     * cannot access protected features of the application.
     */
    data object Unauthenticated : SessionState()
} 