package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember
import com.familystalking.app.data.repository.FriendshipRequest

interface FamilyRepository {
    suspend fun getFamilyMembers(): List<FamilyMember>
    suspend fun getCurrentUser(): FamilyMember
    suspend fun getCurrentUserId(): String?
    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>
    suspend fun acceptFriendshipRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendshipRequest(requestId: String): Result<Unit>
    suspend fun getPendingFriendshipRequests(): List<FriendshipRequest>
} 