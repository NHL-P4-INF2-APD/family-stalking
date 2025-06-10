package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember
import kotlinx.coroutines.flow.Flow

data class PendingRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val timestamp: String
)

interface FamilyRepository {
    suspend fun getFamilyMembers(): List<FamilyMember>
    suspend fun getCurrentUser(): FamilyMember
    suspend fun getCurrentUserId(): String?
    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>
    suspend fun acceptFriendshipRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendshipRequest(requestId: String): Result<Unit>
    fun getPendingFriendshipRequests(): Flow<List<PendingRequest>>
} 