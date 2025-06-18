package com.familystalking.app.presentation.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgendaViewModel @Inject constructor() : ViewModel() {

    companion object {
        // Shared mock list for demo purposes
        private val sharedMockAgendaItems = MutableStateFlow<List<AgendaItem>>(emptyList())
    }

    private val _agendaItems = sharedMockAgendaItems.asStateFlow() // Expose as StateFlow
    val agendaItems: StateFlow<List<AgendaItem>> = _agendaItems

    private val _selectedAgendaItem = MutableStateFlow<AgendaItem?>(null)
    val selectedAgendaItem: StateFlow<AgendaItem?> = _selectedAgendaItem.asStateFlow()

    init {
        // Load initial mock data only if the shared list is empty
        if (sharedMockAgendaItems.value.isEmpty()) {
            loadInitialAgendaItems()
        }
    }

    private fun loadInitialAgendaItems() {
        viewModelScope.launch {
            sharedMockAgendaItems.value = listOf(
                AgendaItem(
                    id = UUID.randomUUID().toString(),
                    title = "Avond eten",
                    dateLabel = "Today",
                    time = "08:00 pm",
                    participants = listOf("Bert", "Peter"),
                    description = "Gezellig samen avondeten. We gaan heerlijke pasta maken.",
                    location = "Woonkamer"
                ),
                AgendaItem(
                    id = UUID.randomUUID().toString(),
                    title = "Uitje naar de dierentuin",
                    dateLabel = "Tomorrow",
                    time = "02:30 pm",
                    participants = listOf("Familie Jansen", "Anna"),
                    description = "Een leuk dagje uit.",
                    location = "Dierentuin Blijdorp"
                )
            )
        }
    }

    fun addAgendaEvent(
        title: String,
        date: LocalDate,
        time: LocalTime,
        location: String?,
        participants: List<String>,
        description: String?
    ) {
        val newItem = AgendaItem(
            id = UUID.randomUUID().toString(),
            title = title,
            dateLabel = formatDateLabel(date),
            time = formatTime(time),
            participants = participants,
            description = description ?: "Event details for $title",
            location = location
        )
        sharedMockAgendaItems.value = sharedMockAgendaItems.value + newItem
    }

    private fun formatDateLabel(date: LocalDate): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return when (date) {
            today -> "Today"
            tomorrow -> "Tomorrow"
            else -> {
                val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                javaDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH))
            }
        }
    }

    private fun formatTime(time: LocalTime): String {
        val javaTime = java.time.LocalTime.of(time.hour, time.minute)
        return javaTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    }

    fun onAgendaItemClick(item: AgendaItem) {
        _selectedAgendaItem.value = item
    }

    fun dismissAgendaDetailPopup() {
        _selectedAgendaItem.value = null
    }
}