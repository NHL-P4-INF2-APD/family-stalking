package com.familystalking.app.presentation.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.Event
import com.familystalking.app.domain.model.EventAttendee
import com.familystalking.app.domain.repository.AgendaRepository
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val agendaRepository: AgendaRepository,
    private val authRepository: AuthenticationRepository
) : ViewModel() {
    private val _agendaItems = MutableStateFlow<List<Event>>(emptyList())
    val agendaItems: StateFlow<List<Event>> = _agendaItems.asStateFlow()

    private val _selectedAgendaItem = MutableStateFlow<Event?>(null)
    val selectedAgendaItem: StateFlow<Event?> = _selectedAgendaItem.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchAgendaItems()
    }

    fun fetchAgendaItems() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    val events = agendaRepository.getEventsForUser(userId)
                    _agendaItems.value = events
                } else {
                    _agendaItems.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Fout bij ophalen van agenda: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addAgendaEvent(
        title: String,
        date: LocalDate,
        time: LocalTime,
        location: String?,
        participants: List<String>, // userIds
        description: String?
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                val eventId = UUID.randomUUID().toString()
                val startDateTime = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
                val event = Event(
                    id = eventId,
                    title = title,
                    description = description,
                    startTime = startDateTime,
                    endTime = null,
                    location = location,
                    createdBy = userId,
                    participants = emptyList() // wordt gevuld na ophalen
                )
                // Voeg attendees toe: aanmaker + geselecteerde userIds
                val allUserIds = (participants + userId).distinct()
                val attendees = allUserIds.map { uid ->
                    val email = getUserEmail(uid)
                    val name = email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Onbekend"
                    EventAttendee(eventId = eventId, userId = uid, name = name)
                }
                println("[DEBUG] addEvent called with event: $event, attendees: $attendees")
                agendaRepository.addEvent(event, attendees)
                fetchAgendaItems()
            } catch (e: Exception) {
                _error.value = "Fout bij toevoegen van event: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun getUserEmail(userId: String): String? {
        // Simpele Supabase query naar users tabel
        // (zelfde als in AgendaRepositoryImpl)
        return try {
            val users = agendaRepository.javaClass.getDeclaredMethod("getUserEmail", String::class.java)
                .apply { isAccessible = true }
                .invoke(agendaRepository, userId) as? String
            users
        } catch (e: Exception) {
            null
        }
    }

    fun onAgendaItemClick(item: Event) {
        _selectedAgendaItem.value = item
    }

    fun dismissAgendaDetailPopup() {
        _selectedAgendaItem.value = null
    }

    fun clearError() {
        _error.value = null
    }
}