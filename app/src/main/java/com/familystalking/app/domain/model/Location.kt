package com.familystalking.app.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Location(
    val id: UUID,
    val userId: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant
)

data class FamilyMemberLocation(
    val userId: UUID,
    val name: String,
    val location: Location
) {
    /**
     * Returns the number of minutes since the last location update
     */
    fun getMinutesSinceLastUpdate(): Long {
        return ChronoUnit.MINUTES.between(location.timestamp, Instant.now())
    }
    
    /**
     * Returns true if the location is considered stale (older than 5 minutes)
     */
    fun isLocationStale(): Boolean {
        return getMinutesSinceLastUpdate() >= 5
    }
    
    /**
     * Returns a formatted string for displaying time since last update
     */
    fun getFormattedTimeSinceUpdate(): String {
        val minutes = getMinutesSinceLastUpdate()
        return when {
            minutes < 1 -> "now"
            minutes == 1L -> "1 minute ago"
            else -> "$minutes minutes ago"
        }
    }
} 