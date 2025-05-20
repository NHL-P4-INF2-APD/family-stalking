package com.familystalking.app.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    private val _currentUser = MutableStateFlow<FamilyMember?>(null)
    val currentUser: StateFlow<FamilyMember?> = _currentUser.asStateFlow()

    init {
        fetchFamilyMembers()
        fetchCurrentUser()
    }

    private fun fetchFamilyMembers() {
        viewModelScope.launch {
            _familyMembers.value = familyRepository.getFamilyMembers()
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = familyRepository.getCurrentUser()
        }
    }
} 