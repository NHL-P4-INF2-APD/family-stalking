package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.Location
import com.familystalking.app.domain.model.FamilyMemberLocation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LocationRepository {
    /**
     * Updates the current user's location
     */
    suspend fun updateUserLocation(latitude: Double, longitude: Double, batteryLevel: Int? = null)

    /**
     * Gets the latest location for a specific user
     */
    suspend fun getUserLocation(userId: UUID): Location?

    /**
     * Gets a flow of location updates for all family members
     * This will emit new values whenever any family member's location changes
     */
    fun getFamilyMembersLocations(): Flow<List<FamilyMemberLocation>>

    /**
     * Gets the currently authenticated user's ID
     */
    suspend fun getAuthenticatedUserId(): UUID?
} 