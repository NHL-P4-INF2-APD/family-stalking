package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.AuthResult

interface AuthenticationRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun resetPassword(email: String): AuthResult
    suspend fun signOut(): AuthResult
} 