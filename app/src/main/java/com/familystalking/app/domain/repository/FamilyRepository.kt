package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember

// This is the interface file. Ensure it has all the function definitions.
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

// Your PendingRequest data class likely lives here or in a related model file.
// I'm including it for completeness.
data class PendingRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val timestamp: String
)