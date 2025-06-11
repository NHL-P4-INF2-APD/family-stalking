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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import com.familystalking.app.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    val showFriendAddedDialog: Boolean = false,
    val hasPendingRequests: Boolean = false,
    val isLoading: Boolean = false,
    val searchResults: List<FamilyMember> = emptyList()
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val auth: AuthenticationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FamilyScreenState())
    val state: StateFlow<FamilyScreenState> = _state.asStateFlow()

    init {
        Log.d("FriendRequestFlow", "VM: FamilyViewModel initialized by Hilt.")
        fetchCurrentUser()
    }

    fun getCurrentUserId(): String? = _state.value.currentUserId

    fun fetchCurrentUser() {
        viewModelScope.launch {
            Log.d("FriendRequestFlow", "VM: Fetching current user...")
            val user = familyRepository.getCurrentUser()
            // The user's ID can be null if something goes wrong, so we handle that.
            val userId = user.id
            _state.update { it.copy(currentUser = user, currentUserId = userId) }
            Log.d("FriendRequestFlow", "VM: Fetched user: ${user.name} (ID: $userId).")

            if (userId != null) {
                Log.d("FriendRequestFlow", "VM: User ID found, now fetching their requests.")
                fetchPendingRequests(userId) // Pass ID directly to solve race condition
            } else {
                Log.w("FriendRequestFlow", "VM: User ID is null after fetch, cannot get requests.")
            }
        }
    }

    fun fetchFamilyMembers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val members = familyRepository.getFamilyMembers()
            _state.update { it.copy(familyMembers = members, isLoading = false) }
        }
    }

    fun handleScannedQrCode(scannedId: String, scannedName: String) {
        _state.value = _state.value.copy(
            scannedUserId = scannedId,
            scannedUserName = scannedName,
            showAddFriendDialog = true
        )
    }

    fun sendFriendshipRequest() {
        viewModelScope.launch {
            val friendId = _state.value.scannedUserId
            if (friendId == null) {
                _state.update { it.copy(error = "No user scanned.") }
                return@launch
            }
            Log.d("FriendRequestFlow", "VM: sendFriendshipRequest called for user $friendId.")
            _state.update { it.copy(isSendingFriendRequest = true) }
            try {
                familyRepository.sendFriendshipRequest(friendId)
                _state.update { it.copy(isSendingFriendRequest = false, showAddFriendDialog = false, scannedUserId = null, scannedUserName = null) }
            } catch (e: Exception) {
                Log.e("FriendRequestFlow", "Error sending friendship request", e)
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to send request",
                        isSendingFriendRequest = false
                    )
                }
            }
        }
    }

    fun acceptFriendshipRequest(request: PendingRequest) {
        viewModelScope.launch {
            try {
                familyRepository.acceptFriendshipRequest(request)
                // Refresh pending requests and family list
                state.value.currentUserId?.let { fetchPendingRequests(it) }
                fetchFamilyMembers()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to accept request.") }
            }
        }
    }

    fun declineFriendshipRequest(request: PendingRequest) {
        viewModelScope.launch {
            try {
                familyRepository.declineFriendshipRequest(request)
                // Refresh pending requests
                state.value.currentUserId?.let { fetchPendingRequests(it) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to decline request.") }
            }
        }
    }

    fun fetchPendingRequests(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("FriendRequestFlow", "VM: Manually fetching pending requests for user $userId.")
                val requests = familyRepository.getPendingRequests(userId)
                _state.update { it.copy(pendingRequests = requests, hasPendingRequests = requests.isNotEmpty()) }
                Log.d("FriendRequestFlow", "VM: Manual fetch complete. Found ${requests.size} requests.")
            } catch (e: Exception) {
                Log.e("FriendRequestFlow", "VM: Error during manual fetch of pending requests.", e)
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _state.update { it.copy(isLoading = true) }
                val result = familyRepository.searchUsers(query)
                _state.update { it.copy(searchResults = result, isLoading = false) }
            } else {
                _state.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun dismissAddFriendDialog() {
        _state.update { it.copy(showAddFriendDialog = false, error = null, scannedUserId = null, scannedUserName = null) }
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