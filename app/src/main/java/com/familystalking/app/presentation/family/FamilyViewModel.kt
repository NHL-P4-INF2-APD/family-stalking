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
import io.github.jan.supabase.gotrue.auth
import android.util.Log

data class FamilyScreenState(
    val familyMembers: List<FamilyMember> = emptyList(),
    val currentUser: FamilyMember? = null,
    val currentUserId: String? = null,
    val pendingRequests: List<FriendshipRequest> = emptyList(),
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
        observeFriendshipChanges()
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

    private fun observeFriendshipChanges() {
        viewModelScope.launch {
            // Initial fetch to show requests on app start
            fetchPendingRequests()
            // Listen for any database changes and refetch the list
            familyRepository.listenToFriendshipRequestChanges().collect {
                fetchPendingRequests()
            }
        }
    }

    private fun fetchPendingRequests() {
        viewModelScope.launch {
            val currentUserId = _state.value.currentUserId ?: return@launch
            val requests = familyRepository.getPendingRequestsForUser(currentUserId)
            _state.value = _state.value.copy(pendingRequests = requests)
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
        Log.d("FamilyVM", "sendFriendshipRequest called.")
        if (_state.value.isSendingFriendRequest) {
            Log.d("FamilyVM", "Request already in progress, ignoring.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingFriendRequest = true)
            Log.d("FamilyVM", "State updated to isSendingFriendRequest = true")
            val scannedUserId = _state.value.scannedUserId ?: run {
                Log.e("FamilyVM", "scannedUserId is null, aborting.")
                _state.value = _state.value.copy(
                    isSendingFriendRequest = false,
                    error = "No user ID was scanned."
                )
                return@launch
            }

            Log.d("FamilyVM", "Calling repository.sendFriendshipRequest for user ID: $scannedUserId")
            familyRepository.sendFriendshipRequest(scannedUserId)
                .onSuccess {
                    Log.d("FamilyVM", "repository.sendFriendshipRequest succeeded.")
                    _state.value = _state.value.copy(
                        showAddFriendDialog = false,
                        scannedUserId = null,
                        scannedUserName = null,
                        isSendingFriendRequest = false,
                        successMessage = "Friend request sent!"
                    )
                }
                .onFailure { error ->
                    Log.e("FamilyVM", "repository.sendFriendshipRequest failed: ${error.message}")
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to send request. The user may already be your friend.",
                        isSendingFriendRequest = false
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun dismissFriendAddedDialog() {
        _state.value = _state.value.copy(showFriendAddedDialog = false, newlyAddedFriend = null)
    }
} 