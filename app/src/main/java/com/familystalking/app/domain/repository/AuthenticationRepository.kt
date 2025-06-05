package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for handling user authentication operations.
 * 
 * This interface defines the contract for authentication-related operations including
 * user sign-in, sign-up, password management, and session management. It provides
 * a reactive approach to session state tracking through StateFlow and uses the
 * Result pattern for error handling.
 * 
 * Implementations of this interface should handle communication with authentication
 * providers (such as Supabase Auth) and manage user session persistence.
 */
interface AuthenticationRepository {
    /**
     * Observable state flow that emits the current authentication session state.
     * 
     * This flow enables reactive UI updates based on authentication state changes.
     * UI components can observe this flow to automatically respond to login/logout events.
     */
    val sessionState: StateFlow<SessionState>
    
    /**
     * Authenticates a user with their email and password credentials.
     * 
     * @param email The user's email address used for authentication
     * @param password The user's password
     * @return [AuthResult.Success] if authentication succeeds, or [AuthResult.Error] 
     *         containing the specific authentication error if it fails
     */
    suspend fun signIn(email: String, password: String): AuthResult
    
    /**
     * Creates a new user account with the provided email and password.
     * 
     * @param email The email address for the new user account
     * @param password The password for the new user account
     * @return [AuthResult.Success] if account creation succeeds, or [AuthResult.Error]
     *         containing the specific error (e.g., email already in use, weak password)
     */
    suspend fun signUp(email: String, password: String): AuthResult
    
    /**
     * Initiates a password reset process for the specified email address.
     * 
     * This typically sends a password reset email to the user. The user can then
     * follow the instructions in the email to reset their password.
     * 
     * @param email The email address of the account requiring password reset
     * @return [AuthResult.Success] if the reset email was sent successfully, or
     *         [AuthResult.Error] if the operation failed (e.g., email not found)
     */
    suspend fun resetPassword(email: String): AuthResult
    
    /**
     * Signs out the currently authenticated user and invalidates their session.
     * 
     * After successful sign-out, the [sessionState] will transition to
     * [SessionState.Unauthenticated].
     * 
     * @return [AuthResult.Success] if sign-out succeeds, or [AuthResult.Error]
     *         if the operation fails
     */
    suspend fun signOut(): AuthResult
    
    /**
     * Checks the current session validity and updates the session state accordingly.
     * 
     * This method is typically called during app initialization to determine if the
     * user has a valid existing session. It updates the [sessionState] based on the
     * session validity.
     */
    suspend fun checkSession()
} 