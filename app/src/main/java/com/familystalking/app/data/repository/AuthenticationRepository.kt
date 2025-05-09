package com.familystalking.app.data.repository

interface AuthenticationRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
} 