package com.familystalking.app.presentation.family

data class FamilyMember(
    val name: String,
    val status: String,
    val avatar: String? = null // Kan later gebruikt worden voor profielfoto
) 