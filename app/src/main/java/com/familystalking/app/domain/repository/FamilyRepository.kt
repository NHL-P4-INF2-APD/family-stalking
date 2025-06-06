package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember
import com.familystalking.app.data.repository.FriendshipRequest
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    suspend fun getFamilyMembers(): List<FamilyMember>
    suspend fun getCurrentUser(): FamilyMember
    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>
    suspend fun acceptFriendshipRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendshipRequest(requestId: String): Result<Unit>
    fun listenToFriendshipRequestChanges(): Flow<PostgresAction>
    suspend fun getPendingRequestsForUser(userId: String): List<FriendshipRequest>
    suspend fun getCurrentUserId(): String?
    suspend fun getUserById(userId: String): FamilyMember?
} 