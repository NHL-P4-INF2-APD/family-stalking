package com.familystalking.app.domain.repository

import com.familystalking.app.domain.model.Event
import com.familystalking.app.domain.model.EventAttendee
import kotlinx.coroutines.flow.Flow

interface AgendaRepository {
    suspend fun getEventsForUser(userId: String): List<Event>
    suspend fun addEvent(event: Event, attendees: List<EventAttendee>)
    suspend fun getEventAttendees(eventId: String): List<EventAttendee>
} 