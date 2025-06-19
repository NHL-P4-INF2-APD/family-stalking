package com.familystalking.app.data.repository

import android.util.Log
import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.domain.repository.AuthenticationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthenticationRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val sessionState: StateFlow<SessionState> = supabaseClient.auth.sessionStatus
        .map { status ->
            Log.d("AuthRepoImpl", "Supabase SessionStatus changed: $status")
            when (status) {
                is SessionStatus.Authenticated -> SessionState.Authenticated
                is SessionStatus.NotAuthenticated -> SessionState.Unauthenticated
                is SessionStatus.LoadingFromStorage -> SessionState.Loading
                is SessionStatus.NetworkError -> SessionState.Unauthenticated
            }
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SessionState.Loading
        )

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signIn failed", e)
            AuthResult.Error(AuthError.InvalidCredentials)
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): AuthResult {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("username", username)
                }
            }
            Log.i("AuthRepoImpl", "signUp call completed successfully for $email. User created, pending email confirmation.")
            AuthResult.Success

        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signUp failed with an exception", e)
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
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AuthRepoImpl", "signOut failed", e)
            AuthResult.Error(AuthError.UnknownError)
        }
    }

    override suspend fun checkSession() {
        Log.d("AuthRepoImpl", "checkSession() called. Current Supabase session: ${supabaseClient.auth.currentSessionOrNull()}")
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