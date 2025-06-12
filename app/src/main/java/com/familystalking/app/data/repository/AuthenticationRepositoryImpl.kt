package com.familystalking.app.data.repository

import android.util.Log
import com.familystalking.app.domain.model.AuthError // Ensure this import is correct for your project
import com.familystalking.app.domain.model.AuthResult // Ensure this import is correct for your project
import com.familystalking.app.domain.model.SessionState // Ensure this import is correct for your project
import com.familystalking.app.domain.repository.AuthenticationRepository // Import from DOMAIN
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthenticationRepository {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            _sessionState.value = SessionState.Authenticated
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signIn failed", e)
            _sessionState.value = SessionState.Unauthenticated
            AuthResult.Error(AuthError.InvalidCredentials)
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            val response = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            if (response?.id != null) {
                Log.i("AuthRepoImpl", "signUp successful for $email, user ID: ${response.id}")
                AuthResult.Success
            } else {
                Log.w("AuthRepoImpl", "signUp returned null or no ID for $email")
                _sessionState.value = SessionState.Unauthenticated
                AuthResult.Error(AuthError.UnknownError)
            }
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signUp failed", e)
            _sessionState.value = SessionState.Unauthenticated
            when {
                e.message?.contains("User already registered", ignoreCase = true) == true ||
                        e.message?.contains("already_exists", ignoreCase = true) == true -> {
                    AuthResult.Error(AuthError.EmailAlreadyInUse)
                }
                else -> AuthResult.Error(AuthError.UnknownError)
            }
        }
    }

    override suspend fun resetPassword(email: String): AuthResult {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "resetPassword failed", e)
            AuthResult.Error(AuthError.InvalidEmail)
        }
    }

    override suspend fun signOut(): AuthResult {
        return try {
            supabaseClient.auth.signOut()
            _sessionState.value = SessionState.Unauthenticated
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signOut failed", e)
            AuthResult.Error(AuthError.UnknownError)
        }
    }

    override suspend fun checkSession() {
        _sessionState.value = try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                Log.d("AuthRepoImpl", "Session active for user: ${session.user?.id}")
                SessionState.Authenticated
            } else {
                Log.d("AuthRepoImpl", "No active session.")
                SessionState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "checkSession failed", e)
            SessionState.Unauthenticated
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return try {
            val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            if (userId != null) {
                Log.d("AuthRepoImpl", "Current User ID: $userId")
            } else {
                Log.w("AuthRepoImpl", "Current User ID is null (no active session or user info).")
            }
            userId
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "Error getting current user ID", e)
            null
        }
    }
}