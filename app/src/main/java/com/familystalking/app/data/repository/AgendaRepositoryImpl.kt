package com.familystalking.app.data.repository

import com.familystalking.app.domain.model.Event
import com.familystalking.app.domain.model.EventAttendee
import com.familystalking.app.domain.repository.AgendaRepository
import com.familystalking.app.domain.repository.FamilyRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CalendarEventRow(
    @SerialName("calendar_id") val calendarId: String,
    @SerialName("family_id") val familyId: String?,
    val title: String,
    val description: String?,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val location: String?,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class EventAttendeeRow(
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    val status: String? = null
)

class AgendaRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val familyRepository: FamilyRepository
) : AgendaRepository {
    override suspend fun getEventsForUser(userId: String): List<Event> {
        println("[DEBUG] getEventsForUser called for userId: $userId")
        
        // Haal alle calendar_ids op waar user attendee is
        val attendeeRows = try {
            supabaseClient.from("event_attendees").select {
                filter { eq("user_id", userId) }
            }.decodeList<EventAttendeeRow>()
        } catch (e: Exception) {
            println("[ERROR] Fout bij ophalen event_attendees: ${e.message}")
            return emptyList()
        }
        
        println("[DEBUG] Found ${attendeeRows.size} attendee rows: $attendeeRows")
        val calendarIds = attendeeRows.map { it.eventId }
        println("[DEBUG] Calendar IDs to lookup: $calendarIds")
        
        if (calendarIds.isEmpty()) {
            println("[DEBUG] No calendar IDs found, returning empty list")
            return emptyList()
        }
        
        // Haal alle events op voor deze calendarIds
        val eventRows = try {
            supabaseClient.from("calendar_events").select {
                filter { isIn("calendar_id", calendarIds) }
            }.decodeList<CalendarEventRow>()
        } catch (e: Exception) {
            println("[ERROR] Fout bij ophalen calendar_events: ${e.message}")
            return emptyList()
        }
        
        println("[DEBUG] Found ${eventRows.size} event rows: $eventRows")
        
        return eventRows.map { row ->
            val attendees = getEventAttendees(row.calendarId)
            println("[DEBUG] Event ${row.calendarId} heeft ${attendees.size} attendees: $attendees")
            val startTime = Instant.parse(row.startTime).toLocalDateTime(TimeZone.currentSystemDefault())
            val endTime = Instant.parse(row.endTime).toLocalDateTime(TimeZone.currentSystemDefault())
            Event(
                id = row.calendarId,
                title = row.title,
                description = row.description,
                startTime = startTime,
                endTime = endTime,
                location = row.location,
                createdBy = row.createdBy,
                participants = attendees.map { it.name }
            )
        }
    }

    override suspend fun addEvent(event: Event, attendees: List<EventAttendee>) {
        try {
            // Voeg event toe
            val eventInsertResponse = supabaseClient.from("calendar_events").insert(
                CalendarEventRow(
                    calendarId = event.id,
                    familyId = null,
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime.toString(),
                    endTime = event.endTime.toString(),
                    location = event.location,
                    createdBy = event.createdBy
                )
            )
            println("[DEBUG] Event insert response: $eventInsertResponse")
            // Voeg alle attendees toe
            attendees.forEach { attendee ->
                val attendeeInsertResponse = supabaseClient.from("event_attendees").insert(
                    EventAttendeeRow(
                        eventId = event.id,
                        userId = attendee.userId
                    )
                )
                println("[DEBUG] Attendee insert response: $attendeeInsertResponse")
            }
        } catch (e: Exception) {
            println("[ERROR] Fout bij toevoegen van event of attendees: ${e.message}")
            throw e
        }
    }

    override suspend fun getEventAttendees(eventId: String): List<EventAttendee> {
        val attendeeRows = supabaseClient.from("event_attendees").select {
            filter { eq("event_id", eventId) }
        }.decodeList<EventAttendeeRow>()
        // Haal family members op en maak een map van userId naar gebruikersnaam
        val familyMembers = familyRepository.getFamilyMembers()
        val userIdToName = familyMembers.associate { it.id to it.name }
        val attendees = attendeeRows.mapNotNull { row ->
            val name = userIdToName[row.userId]
            if (name != null) {
                EventAttendee(
                    eventId = row.eventId,
                    userId = row.userId,
                    name = name
                )
            } else {
                null
            }
        }.toMutableList()
        // Voeg de huidige gebruiker toe als deze nog niet in de lijst zit
        val currentUser = familyRepository.getCurrentUser()
        val alreadyInList = attendees.any { it.userId == currentUser.id }
        if (!alreadyInList && attendeeRows.any { it.userId == currentUser.id }) {
            attendees.add(
                EventAttendee(
                    eventId = eventId,
                    userId = currentUser.id ?: "",
                    name = currentUser.name
                )
            )
        }
        return attendees
    }
} 