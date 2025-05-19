package com.familystalking.app.presentation.family

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.familystalking.app.presentation.family.FamilyMember

class FamilyViewModel : ViewModel() {
    private val _familyMembers = MutableStateFlow(
        listOf(
            FamilyMember("Bert", "Driving"),
            FamilyMember("Peter", "At school"),
            FamilyMember("Thijn", "At work")
        )
    )
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()
} 