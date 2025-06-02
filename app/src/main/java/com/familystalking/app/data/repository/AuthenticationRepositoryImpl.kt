package com.familystalking.app.data.repository

import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.domain.repository.AuthenticationRepository
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
            e.printStackTrace() // Add logging for debugging
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
            
            // Check if signup was successful and user is confirmed
            if (response?.id != "") {
                _sessionState.value = SessionState.Authenticated
                AuthResult.Success
            } else {
                _sessionState.value = SessionState.Unauthenticated
                AuthResult.Error(AuthError.UnknownError)
            }
        } catch (e: Exception) {
            e.printStackTrace() // Add logging for debugging
            _sessionState.value = SessionState.Unauthenticated
            when {
                e.message?.contains("already", ignoreCase = true) == true -> {
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
            e.printStackTrace() // Add logging for debugging
            AuthResult.Error(AuthError.InvalidEmail)
        }
    }

    override suspend fun signOut(): AuthResult {
        return try {
            supabaseClient.auth.signOut()
            _sessionState.value = SessionState.Unauthenticated
            AuthResult.Success
        } catch (e: Exception) {
            e.printStackTrace() // Add logging for debugging
            AuthResult.Error(AuthError.UnknownError)
        }
    }

    override suspend fun checkSession() {
        _sessionState.value = try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                SessionState.Authenticated
            } else {
                SessionState.Unauthenticated
            }
        } catch (e: Exception) {
            e.printStackTrace() // Add logging for debugging
            SessionState.Unauthenticated
        }
    }
} 