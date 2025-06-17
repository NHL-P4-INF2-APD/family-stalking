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
        Log.e("FAMILY_VIEW_MODEL_INIT_TEST", "--- FamilyViewModel INIT BLOCK HAS DEFINITELY RUN ---") // MOVED TO VERY TOP
        observeAuthStateAndLoadData()
    }

    private fun observeAuthStateAndLoadData() {
        Log.e("FamilyViewModel_OBSERVE", "--- observeAuthStateAndLoadData CALLED ---")
        viewModelScope.launch {
            authenticationRepository.sessionState.collectLatest { sessionState ->
                Log.e("FamilyViewModel_SESSION", "--- Observed Auth Session State: $sessionState ---")
                when (sessionState) {
                    is SessionState.Authenticated -> {
                        Log.e("FamilyViewModel_AUTHED", "--- State is Authenticated ---")
                        val authUserId = authenticationRepository.getCurrentUserId()
                        Log.e("FamilyViewModel_AUTHED", "--- Auth User ID from AuthRepo: $authUserId ---")

                        if (authUserId != null) {
                            if (_state.value.currentUserId != authUserId || _state.value.currentUser == null) {
                                Log.e("FamilyViewModel_AUTHED", "--- Conditions met to fetch/refresh user data for $authUserId ---")
                                fetchCurrentUserAndInitialDataInternal(authUserId)
                            } else {
                                Log.d("FamilyViewModel", "OBSERVE_AUTH: User data for $authUserId seems current. isLoading: ${_state.value.isLoading}")
                                if (_state.value.isLoading && _state.value.currentUser != null) {
                                    _state.update { it.copy(isLoading = false) }
                                }
                            }
                        } else {
                            Log.w("FamilyViewModel", "OBSERVE_AUTH: Auth state is Authenticated, but getCurrentUserId returned null. Clearing data.")
                            _state.update { FamilyScreenState(isLoading = false) }
                        }
                    }
                    is SessionState.Unauthenticated -> {
                        Log.e("FamilyViewModel_UNAUTH", "--- State is Unauthenticated. Clearing user specific data. ---")
                        _state.update { FamilyScreenState(isLoading = false) }
                    }
                    is SessionState.Loading -> {
                        Log.e("FamilyViewModel_LOADING", "--- Auth state is Loading. Setting global isLoading true. ---")
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun fetchCurrentUserAndInitialDataInternal(authenticatedUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            Log.e("FamilyViewModel_FETCH_INT", "--- fetchCurrentUserAndInitialDataInternal called for authUserId: $authenticatedUserId ---")

            val user = familyRepository.getCurrentUser()
            val userId = user.id
            Log.e("FamilyViewModel_FETCH_INT", "--- Fetched user profile from repo: Name='${user.name}', ID='${user.id}' ---")
            _state.update { it.copy(currentUser = user, currentUserId = userId) }

            if (user.id != null) {
                Log.d("FamilyViewModel", "FETCH_INTERNAL: User profile ID (${user.id}) is not null. Fetching pending requests and family members.")
                fetchPendingRequests(user.id!!)
                Log.d("FamilyViewModel", "FETCH_INTERNAL: About to call fetchFamilyMembers()")
                fetchFamilyMembers()
            } else {
                Log.w("FamilyViewModel", "FETCH_INTERNAL: User profile ID is null after fetching current user. Cannot fetch related data.")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchFamilyMembers() {
        viewModelScope.launch {
            Log.d("FamilyViewModel", "[fetchFamilyMembers] START - Current state: familyMembers=${_state.value.familyMembers.size}, currentUserId=${_state.value.currentUserId}")
            _state.update { it.copy(isLoading = true) }
            try {
                val members = familyRepository.getFamilyMembers()
                Log.d("FamilyViewModel", "[fetchFamilyMembers] Repository returned ${members.size} members")
                members.forEach { member ->
                    Log.d("FamilyViewModel", "[fetchFamilyMembers] Member: id=${member.id}, name=${member.name}, status=${member.status}")
                }
                _state.update { it.copy(familyMembers = members, isLoading = false) }
                Log.d("FamilyViewModel", "[fetchFamilyMembers] State updated with ${members.size} family members.")
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
            Log.d("FamilyViewModel", "[acceptFriendshipRequest] START - Accepting request from ${request.senderName} (ID: ${request.senderId})")
            _state.update { it.copy(isLoading = true) }
            val result = familyRepository.acceptFriendshipRequest(request)
            if (result.isSuccess) {
                Log.d("FamilyViewModel", "[acceptFriendshipRequest] SUCCESS - Request accepted, now refreshing data")
                _state.value.currentUserId?.let {
                    Log.d("FamilyViewModel", "[acceptFriendshipRequest] Fetching pending requests for user: $it")
                    fetchPendingRequests(it)
                }
                Log.d("FamilyViewModel", "[acceptFriendshipRequest] Now fetching family members...")
                fetchFamilyMembers()
                _state.update { it.copy(successMessage = "Friend request from ${request.senderName} accepted!") }
            } else {
                Log.e("FamilyViewModel", "[acceptFriendshipRequest] FAILED - ${result.exceptionOrNull()?.message}")
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
                Log.i("FamilyViewModel", "[fetchPendingRequests] Calling repository with userId: $userId.")
                val requests = familyRepository.getPendingRequests(userId)
                _state.update { it.copy(pendingRequests = requests, hasPendingRequests = requests.isNotEmpty()) }
                Log.i("FamilyViewModel", "[fetchPendingRequests] Found ${requests.size} requests. HasPending: ${requests.isNotEmpty()}")
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