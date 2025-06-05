package com.familystalking.app.presentation.agenda

data class AgendaItem(
    val id: String,
    val title: String,
    val dateLabel: String,
    val time: String,
    val participants: List<String>,
    val description: String,
    val location: String? = null
)