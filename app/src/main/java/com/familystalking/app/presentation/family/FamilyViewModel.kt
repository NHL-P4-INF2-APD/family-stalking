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
    val requestAlreadyPendingMessage: String = ""
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FamilyScreenState())
    val state: StateFlow<FamilyScreenState> = _state.asStateFlow()

    init {
        Log.d("FamilyViewModel", "VM: FamilyViewModel initialized by Hilt.")
        observeAuthStateAndLoadData()
    }

    private fun observeAuthStateAndLoadData() {
        viewModelScope.launch {
            authenticationRepository.sessionState.collectLatest { sessionState ->
                Log.d("FamilyViewModel", "VM: Observed Auth Session State: $sessionState")
                if (sessionState is SessionState.Authenticated) {
                    val authUserId = authenticationRepository.getCurrentUserId()
                    if (_state.value.currentUserId != authUserId || _state.value.currentUser == null) {
                        Log.d("FamilyViewModel", "VM: Auth state is Authenticated. Fetching current user and initial data for $authUserId.")
                        fetchCurrentUserAndInitialDataInternal()
                    } else {
                        Log.d("FamilyViewModel", "VM: Auth state is Authenticated, but user data seems current. CurrentUserID: ${_state.value.currentUserId}")
                        if (_state.value.isLoading) { // If was loading due to previous state, ensure it's false
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
                } else if (sessionState is SessionState.Unauthenticated) {
                    Log.d("FamilyViewModel", "VM: Auth state is Unauthenticated. Clearing user specific data.")
                    _state.update { FamilyScreenState(isLoading = false) }
                } else if (sessionState is SessionState.Loading) {
                    Log.d("FamilyViewModel", "VM: Auth state is Loading. Setting isLoading true.")
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun fetchCurrentUserAndInitialDataInternal() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            Log.d("FamilyViewModel", "VM: fetchCurrentUserAndInitialDataInternal called.")

            val user = familyRepository.getCurrentUser()
            val userId = user.id
            Log.d("FamilyViewModel", "VM: Fetched user from repo: ${user.name} (ID: $userId).")
            _state.update { it.copy(currentUser = user, currentUserId = userId) }

            if (userId != null) {
                Log.d("FamilyViewModel", "VM: User ID ($userId) is not null. Fetching pending requests and family members.")
                fetchPendingRequests(userId)
                fetchFamilyMembers()
            } else {
                Log.w("FamilyViewModel", "VM: User ID is null after fetching current user. Cannot fetch related data.")
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
                Log.d("FamilyViewModel", "[fetchFamilyMembers] Fetched ${members.size} family members.")
            } catch (e: Exception) {
                Log.e("FamilyViewModel", "[fetchFamilyMembers] Error fetching family members", e)
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
            val receiverId = _state.value.scannedUserId
            if (receiverId == null) {
                _state.update { it.copy(error = "No user selected to send request.") }
                return@launch
            }

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
                Log.d("FamilyViewModel", "[fetchPendingRequests] Calling repository with userId: $userId.")
                val requests = familyRepository.getPendingRequests(userId)
                _state.update { it.copy(pendingRequests = requests, hasPendingRequests = requests.isNotEmpty()) }
                Log.d("FamilyViewModel", "[fetchPendingRequests] Found ${requests.size} requests. HasPending: ${requests.isNotEmpty()}")
            } catch (e: Exception) {
                Log.e("FamilyViewModel", "[fetchPendingRequests] Error: ${e.message}", e)
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