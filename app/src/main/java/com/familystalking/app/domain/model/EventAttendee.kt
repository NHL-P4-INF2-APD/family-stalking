package com.familystalking.app.domain.model

// Domeinmodel voor een event attendee
data class EventAttendee(
    val eventId: String,
    val userId: String,
    val name: String // afgeleid van e-mail voor de @
) 