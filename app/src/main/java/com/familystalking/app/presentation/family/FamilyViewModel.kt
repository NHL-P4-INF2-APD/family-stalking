package com.familystalking.app.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.domain.repository.PendingRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class FamilyScreenState(
    val familyMembers: List<FamilyMember> = emptyList(),
    val currentUser: FamilyMember? = null,
    val currentUserId: String? = null,
    val pendingRequests: List<PendingRequest> = emptyList(),
    val showAddFriendDialog: Boolean = false,
    val scannedUserId: String? = null,
    val scannedUserName: String? = null,
    val isSendingFriendRequest: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val hasPendingRequests: Boolean = false,
    val isLoading: Boolean = true,
    val searchResults: List<FamilyMember> = emptyList(),
    val showRequestAlreadyPendingDialog: Boolean = false,
    val requestAlreadyPendingMessage: String = "",
    val showConfirmUnfriendDialog: Boolean = false,
    val userToUnfriend: FamilyMember? = null
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FamilyScreenState())
    val state: StateFlow<FamilyScreenState> = _state.asStateFlow()

    init {
        observeAuthStateAndLoadData()
    }

    private fun observeAuthStateAndLoadData() {
        viewModelScope.launch {
            authenticationRepository.sessionState.collectLatest { sessionState ->
                when (sessionState) {
                    is SessionState.Authenticated -> {
                        val authUserId = authenticationRepository.getCurrentUserId()
                        if (authUserId != null) {
                            if (_state.value.currentUserId != authUserId || _state.value.currentUser == null) {
                                fetchCurrentUserAndInitialDataInternal(authUserId)
                            } else {
                                if (_state.value.isLoading && _state.value.currentUser != null) {
                                    _state.update { it.copy(isLoading = false) }
                                }
                            }
                        } else {
                            _state.update { FamilyScreenState(isLoading = false) }
                        }
                    }
                    is SessionState.Unauthenticated -> {
                        _state.update { FamilyScreenState(isLoading = false) }
                    }
                    is SessionState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun fetchCurrentUserAndInitialDataInternal(authenticatedUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = familyRepository.getCurrentUser()
            _state.update { it.copy(currentUser = user, currentUserId = user.id) }

            if (user.id != null) {
                fetchPendingRequests(user.id)
                fetchFamilyMembers()
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchFamilyMembers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val members = familyRepository.getFamilyMembers()
                _state.update { it.copy(familyMembers = members, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not load family members.", isLoading = false) }
            }
        }
    }

    fun handleScannedQrCode(scannedId: String, scannedName: String) {
        _state.update {
            it.copy(
                scannedUserId = scannedId,
                scannedUserName = scannedName,
                showAddFriendDialog = true,
                error = null,
                successMessage = null
            )
        }
    }

    fun sendFriendshipRequest() {
        viewModelScope.launch {
            val receiverId = _state.value.scannedUserId ?: return@launch

            _state.update { it.copy(isSendingFriendRequest = true, error = null, successMessage = null) }
            val result = familyRepository.sendFriendshipRequest(receiverId)

            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        successMessage = "Friend request sent to ${_state.value.scannedUserName ?: "user"}!",
                        isSendingFriendRequest = false,
                        showAddFriendDialog = false,
                        scannedUserId = null,
                        scannedUserName = null
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = when (exception?.message) {
                    "FRIEND_REQUEST_ALREADY_PENDING_SENT" -> {
                        _state.update { it.copy(showRequestAlreadyPendingDialog = true, requestAlreadyPendingMessage = "You've already sent a friend request to ${_state.value.scannedUserName ?: "this user"}. It's still pending.") }
                        null
                    }
                    "FRIEND_REQUEST_ALREADY_PENDING_RECEIVED" -> {
                        _state.update { it.copy(showRequestAlreadyPendingDialog = true, requestAlreadyPendingMessage = "${_state.value.scannedUserName ?: "This user"} has already sent you a friend request. Check your pending requests to accept it.") }
                        null
                    }
                    "You are already friends with this user." -> "You are already friends with ${_state.value.scannedUserName ?: "this user"}."
                    "You cannot add yourself as a friend." -> "You cannot add yourself as a friend."
                    else -> exception?.message ?: "Failed to send request."
                }
                if (errorMessage != null) {
                    _state.update { it.copy(error = errorMessage, isSendingFriendRequest = false, showAddFriendDialog = false) }
                } else {
                    _state.update { it.copy(isSendingFriendRequest = false, showAddFriendDialog = false) }
                }
            }
        }
    }

    fun dismissRequestAlreadyPendingDialog() {
        _state.update { it.copy(showRequestAlreadyPendingDialog = false, requestAlreadyPendingMessage = "", scannedUserId = null, scannedUserName = null) }
    }

    fun acceptFriendshipRequest(request: PendingRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = familyRepository.acceptFriendshipRequest(request)
            if (result.isSuccess) {
                _state.value.currentUserId?.let { fetchPendingRequests(it) }
                fetchFamilyMembers()
                _state.update { it.copy(successMessage = "Friend request from ${request.senderName} accepted!") }
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to accept request.", isLoading = false) }
            }
        }
    }

    fun declineFriendshipRequest(request: PendingRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = familyRepository.declineFriendshipRequest(request)
            if (result.isSuccess) {
                _state.value.currentUserId?.let { fetchPendingRequests(it) }
                _state.update { it.copy(successMessage = "Friend request from ${request.senderName} declined.", isLoading = false) }
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to decline request.", isLoading = false) }
            }
        }
    }

    fun fetchPendingRequests(userId: String) {
        viewModelScope.launch {
            try {
                val requests = familyRepository.getPendingRequests(userId)
                _state.update { it.copy(pendingRequests = requests, hasPendingRequests = requests.isNotEmpty()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not load pending requests.") }
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.trim().length > 2) {
                _state.update { it.copy(isLoading = true) }
                val results = familyRepository.searchUsers(query.trim())
                _state.update { it.copy(searchResults = results, isLoading = false) }
            } else {
                _state.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun onUnfriendClick(member: FamilyMember) {
        _state.update { it.copy(showConfirmUnfriendDialog = true, userToUnfriend = member) }
    }

    fun dismissUnfriendDialog() {
        _state.update { it.copy(showConfirmUnfriendDialog = false, userToUnfriend = null) }
    }

    fun confirmUnfriend() {
        viewModelScope.launch {
            val friendToUnfriend = _state.value.userToUnfriend ?: return@launch
            _state.update { it.copy(showConfirmUnfriendDialog = false, isLoading = true) }
            val result = familyRepository.removeFriend(friendToUnfriend.id!!)
            if (result.isSuccess) {
                _state.update { it.copy(successMessage = "${friendToUnfriend.name} has been removed from your friends.") }
                fetchFamilyMembers()
            } else {
                _state.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to remove friend.",
                        isLoading = false
                    )
                }
            }
            _state.update { it.copy(userToUnfriend = null) }
        }
    }

    fun dismissAddFriendDialog() {
        _state.update { it.copy(showAddFriendDialog = false, error = null, scannedUserId = null, scannedUserName = null, searchResults = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }
}