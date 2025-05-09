package com.familystalking.app.data.repository

import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.repository.AuthenticationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthenticationRepository {

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(AuthError.InvalidCredentials)
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(AuthError.EmailAlreadyInUse)
        }
    }

    override suspend fun resetPassword(email: String): AuthResult {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(AuthError.InvalidEmail)
        }
    }
} 