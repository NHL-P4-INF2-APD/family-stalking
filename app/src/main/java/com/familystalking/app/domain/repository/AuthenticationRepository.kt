package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

interface AuthenticationRepository {
    val sessionState: StateFlow<SessionState>
    
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun resetPassword(email: String): AuthResult
    suspend fun signOut(): AuthResult
    suspend fun checkSession()
} 