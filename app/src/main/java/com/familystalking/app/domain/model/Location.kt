package com.familystalking.app.domain.model

import java.time.Instant
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
) 