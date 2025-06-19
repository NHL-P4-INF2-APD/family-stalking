package com.familystalking.app.domain.model

import kotlinx.datetime.LocalDateTime

// Domeinmodel voor een agenda event
// Deelnemers zijn namen (geen ID's)
data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val createdBy: String, // user_id van aanmaker
    val participants: List<String> // lijst van namen
) 