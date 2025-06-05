package com.familystalking.app.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a geographical location with timestamp information.
 * 
 * This data class encapsulates location data for tracking family members' positions
 * within the Family Stalking application. Each location entry is associated with
 * a specific user and includes precise coordinates and timestamp information.
 * 
 * @property id Unique identifier for this location entry
 * @property userId The unique identifier of the user associated with this location
 * @property latitude The latitude coordinate in decimal degrees (-90.0 to 90.0)
 * @property longitude The longitude coordinate in decimal degrees (-180.0 to 180.0)
 * @property timestamp The exact time when this location was recorded
 */
data class Location(
    val id: UUID,
    val userId: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant
)

/**
 * Represents a family member's current location with their identifying information.
 * 
 * This data class combines user identity information with their location data,
 * providing a complete picture of a family member's current position. Used primarily
 * for displaying family members on maps and location-based features.
 * 
 * @property userId The unique identifier of the family member
 * @property name The display name of the family member
 * @property location The current geographical location and timestamp information for this family member
 */
data class FamilyMemberLocation(
    val userId: UUID,
    val name: String,
    val location: Location
) 