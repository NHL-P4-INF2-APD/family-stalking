package com.familystalking.app.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.data.repository.FriendshipRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyScreenState(
    val familyMembers: List<FamilyMember> = emptyList(),
    val currentUser: FamilyMember? = null,
    val currentUserId: String? = null,
    val pendingRequests: List<FriendshipRequest> = emptyList(),
    val showAddFriendDialog: Boolean = false,
    val showRequestDialog: Boolean = false,
    val scannedUserId: String? = null,
    val scannedUserName: String? = null,
    val error: String? = null
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FamilyScreenState())
    val state: StateFlow<FamilyScreenState> = _state.asStateFlow()

    init {
        fetchFamilyMembers()
        fetchCurrentUser()
        observePendingRequests()
    }

    fun getCurrentUserId(): String? = _state.value.currentUserId

    private fun fetchFamilyMembers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                familyMembers = familyRepository.getFamilyMembers()
            )
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            val user = familyRepository.getCurrentUser()
            _state.value = _state.value.copy(
                currentUser = user,
                currentUserId = familyRepository.getCurrentUserId()
            )
        }
    }

    private fun observePendingRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                pendingRequests = familyRepository.getPendingFriendshipRequests()
            )
        }
    }

    fun handleScannedQrCode(userId: String, userName: String) {
        _state.value = _state.value.copy(
            scannedUserId = userId,
            scannedUserName = userName,
            showAddFriendDialog = true
        )
    }

    fun sendFriendshipRequest() {
        viewModelScope.launch {
            val scannedUserId = _state.value.scannedUserId ?: return@launch
            familyRepository.sendFriendshipRequest(scannedUserId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        showAddFriendDialog = false,
                        scannedUserId = null,
                        scannedUserName = null
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message
                    )
                }
        }
    }

    fun acceptFriendshipRequest(requestId: String) {
        viewModelScope.launch {
            familyRepository.acceptFriendshipRequest(requestId)
                .onSuccess {
                    observePendingRequests()
                    fetchFamilyMembers()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message
                    )
                }
        }
    }

    fun rejectFriendshipRequest(requestId: String) {
        viewModelScope.launch {
            familyRepository.rejectFriendshipRequest(requestId)
                .onSuccess {
                    observePendingRequests()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message
                    )
                }
        }
    }

    fun dismissAddFriendDialog() {
        _state.value = _state.value.copy(
            showAddFriendDialog = false,
            scannedUserId = null,
            scannedUserName = null
        )
    }

    fun dismissRequestDialog() {
        _state.value = _state.value.copy(
            showRequestDialog = false
        )
    }
} 