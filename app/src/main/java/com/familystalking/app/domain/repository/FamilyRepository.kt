package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember

interface FamilyRepository {

    suspend fun getFamilyMembers(): List<FamilyMember>

    suspend fun getCurrentUser(): FamilyMember

    suspend fun getCurrentUserId(): String?

    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>

    suspend fun acceptFriendshipRequest(request: PendingRequest): Result<Unit>

    suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit>

    suspend fun getPendingRequests(userId: String): List<PendingRequest>

    suspend fun searchUsers(query: String): List<FamilyMember>

    // ADD THIS LINE
    suspend fun removeFriend(friendId: String): Result<Unit>
}

data class PendingRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val timestamp: String
)