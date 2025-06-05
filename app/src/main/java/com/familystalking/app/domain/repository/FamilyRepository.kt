package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember
import com.familystalking.app.data.repository.FriendshipRequest

/**
 * Repository interface for managing family members and friendship relationships.
 * 
 * This interface defines the contract for family-related operations including
 * retrieving family members, managing friendship requests, and handling user
 * relationships within the Family Stalking application. It facilitates the
 * social aspects of the app by managing connections between family members.
 */
interface FamilyRepository {
    /**
     * Retrieves a list of all family members associated with the current user.
     * 
     * This includes all users who have accepted friendship requests or are part
     * of the user's family network.
     * 
     * @return A list of [FamilyMember] objects representing all connected family members
     */
    suspend fun getFamilyMembers(): List<FamilyMember>
    
    /**
     * Retrieves the current authenticated user's information as a family member.
     * 
     * @return [FamilyMember] object containing the current user's profile information
     */
    suspend fun getCurrentUser(): FamilyMember
    
    /**
     * Gets the unique identifier of the currently authenticated user.
     * 
     * @return The current user's ID as a string, or null if no user is authenticated
     */
    suspend fun getCurrentUserId(): String?
    
    /**
     * Sends a friendship request to another user.
     * 
     * This creates a pending friendship request that the recipient can either
     * accept or reject. The request will appear in the recipient's pending requests list.
     * 
     * @param receiverId The unique identifier of the user to send the friendship request to
     * @return [Result] indicating success or containing an error if the operation fails
     */
    suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>
    
    /**
     * Accepts a pending friendship request.
     * 
     * This establishes a bidirectional friendship relationship between the current user
     * and the request sender, allowing them to see each other's locations and be part
     * of the same family network.
     * 
     * @param requestId The unique identifier of the friendship request to accept
     * @return [Result] indicating success or containing an error if the operation fails
     */
    suspend fun acceptFriendshipRequest(requestId: String): Result<Unit>
    
    /**
     * Rejects a pending friendship request.
     * 
     * This permanently denies the friendship request and removes it from the
     * pending requests list. The request sender will not be notified of the rejection.
     * 
     * @param requestId The unique identifier of the friendship request to reject
     * @return [Result] indicating success or containing an error if the operation fails
     */
    suspend fun rejectFriendshipRequest(requestId: String): Result<Unit>
    
    /**
     * Retrieves all pending friendship requests for the current user.
     * 
     * This returns requests that have been sent to the current user but have not
     * yet been accepted or rejected. Users can review and respond to these requests.
     * 
     * @return A list of [FriendshipRequest] objects representing all pending requests
     */
    suspend fun getPendingFriendshipRequests(): List<FriendshipRequest>
} 