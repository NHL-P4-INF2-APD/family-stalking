package com.familystalking.app.domain.repository

// This FamilyMember likely needs to be defined somewhere, probably in domain.model
// e.g., data class FamilyMember(val id: String, val name: String, val status: String = "Offline", ...)
import com.familystalking.app.presentation.family.FamilyMember // Is this the right location for the model?

// This PendingRequest is already defined in this file, which is fine.
data class PendingRequest(
    val id: String, // Unique ID of the request itself
    val senderId: String,
    val senderName: String, // Denormalized for easier display
    val timestamp: String // Consider using a proper timestamp type like kotlinx.datetime.Instant
)

interface FamilyRepository {
    suspend fun getFamilyMembers(): List<FamilyMember> // Get confirmed friends
    suspend fun getCurrentUser(): FamilyMember // Fetches current user's profile as FamilyMember
    suspend fun getCurrentUserId(): String? // This seems redundant if getCurrentUser().id exists. Is it from auth?
    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> // Standard Result type is good
    suspend fun acceptFriendshipRequest(request: PendingRequest): Result<Unit>
    suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit>
    suspend fun getPendingRequests(userId: String): List<PendingRequest> // Get requests where userId is the receiver
    suspend fun searchUsers(query: String): List<FamilyMember> // Search for users to add
}