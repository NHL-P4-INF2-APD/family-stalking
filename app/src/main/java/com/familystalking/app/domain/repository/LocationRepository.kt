package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.Location
import com.familystalking.app.domain.model.FamilyMemberLocation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository interface for managing location-related operations.
 * 
 * This interface defines the contract for location tracking functionality within
 * the Family Stalking application. It handles user location updates, retrieval
 * of family member locations, and provides reactive location data streams for
 * real-time location sharing between family members.
 */
interface LocationRepository {
    /**
     * Updates the current user's location in the system.
     * 
     * This method records the user's current geographical position with a timestamp,
     * making it available to other family members for location sharing purposes.
     * 
     * @param latitude The latitude coordinate in decimal degrees (valid range: -90.0 to 90.0)
     * @param longitude The longitude coordinate in decimal degrees (valid range: -180.0 to 180.0)
     * @throws IllegalStateException if no user is currently authenticated
     */
    suspend fun updateUserLocation(latitude: Double, longitude: Double)

    /**
     * Retrieves the most recent location for a specific user.
     * 
     * This method fetches the latest recorded location entry for the specified user,
     * providing access to their most current known position and the timestamp when it was recorded.
     * 
     * @param userId The unique identifier of the user whose location is being requested
     * @return The most recent [Location] for the specified user, or null if no location data exists
     */
    suspend fun getUserLocation(userId: UUID): Location?

    /**
     * Provides a reactive stream of location updates for all family members.
     * 
     * This flow emits new values whenever any family member's location changes,
     * enabling real-time location tracking and map updates. The flow includes
     * both location data and user identification information for display purposes.
     * 
     * @return A [Flow] that emits lists of [FamilyMemberLocation] objects representing
     *         the current locations of all family members
     */
    fun getFamilyMembersLocations(): Flow<List<FamilyMemberLocation>>

    /**
     * Retrieves the unique identifier of the currently authenticated user.
     * 
     * This method provides access to the current user's ID for location operations
     * and permission checking. Used internally for associating location updates
     * with the correct user account.
     * 
     * @return The authenticated user's unique identifier, or null if no user is authenticated
     */
    suspend fun getAuthenticatedUserId(): UUID?
} 