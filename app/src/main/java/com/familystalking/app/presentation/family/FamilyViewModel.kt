package com.familystalking.app.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.data.repository.FriendshipRequest
import com.familystalking.app.domain.repository.PendingRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.jan.supabase.gotrue.auth
import android.util.Log

data class FamilyScreenState(
    val familyMembers: List<FamilyMember> = emptyList(),
    val currentUser: FamilyMember? = null,
    val currentUserId: String? = null,
    val pendingRequests: List<PendingRequest> = emptyList(),
    val showAddFriendDialog: Boolean = false,
    val showRequestDialog: Boolean = false,
    val scannedUserId: String? = null,
    val scannedUserName: String? = null,
    val isSendingFriendRequest: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val newlyAddedFriend: FamilyMember? = null,
    val showFriendAddedDialog: Boolean = false
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

    fun fetchCurrentUser() {
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
            Log.d("FriendRequestFlow", "VM: Attaching observer for pending requests.")
            familyRepository.getPendingFriendshipRequests()
                .catch { e ->
                    Log.e("FriendRequestFlow", "VM: Flow collection error.", e)
                    _state.update { it.copy(error = "Failed to listen for friend requests: ${e.message}") }
                }
                .collect { requests ->
                    Log.d("FriendRequestFlow", "VM: Received new list of ${requests.size} pending requests.")
                    _state.update { it.copy(pendingRequests = requests) }
                }
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
            val scannedUserId = _state.value.scannedUserId
            Log.d("FriendRequestFlow", "VM: sendFriendshipRequest called for user $scannedUserId.")
            if (scannedUserId == null) {
                Log.e("FriendRequestFlow", "VM: Aborting, scannedUserId is null.")
                return@launch
            }

            _state.update { it.copy(isSendingFriendRequest = true) }
            val result = familyRepository.sendFriendshipRequest(scannedUserId)
            
            if (result.isFailure) {
                Log.e("FriendRequestFlow", "VM: sendFriendshipRequest failed: ${result.exceptionOrNull()?.message}")
            }

            _state.update {
                it.copy(
                    isSendingFriendRequest = false,
                    showAddFriendDialog = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun acceptFriendshipRequest(requestId: String) {
        viewModelScope.launch {
            familyRepository.acceptFriendshipRequest(requestId)
                .onSuccess {
                    fetchFamilyMembers()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message
                    )
                }
        }
        _state.value = _state.value.copy(
            showRequestDialog = false
        )
    }

    fun rejectFriendshipRequest(requestId: String) {
        viewModelScope.launch {
            familyRepository.rejectFriendshipRequest(requestId)
                .onSuccess {
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message
                    )
                }
        }
    }

    fun dismissAddFriendDialog() {
        _state.update {
            it.copy(
                showAddFriendDialog = false,
                scannedUserId = null,
                scannedUserName = null
            )
        }
    }

    fun dismissRequestDialog() {
        _state.value = _state.value.copy(
            showRequestDialog = false
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun dismissFriendAddedDialog() {
        _state.value = _state.value.copy(showFriendAddedDialog = false, newlyAddedFriend = null)
    }
} 