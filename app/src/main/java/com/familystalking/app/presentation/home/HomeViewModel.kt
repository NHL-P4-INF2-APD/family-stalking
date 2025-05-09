package com.familystalking.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut.asStateFlow()

    fun onSignOut() {
        viewModelScope.launch {
            _isLoading.value = true
            authenticationRepository.signOut()
            _isLoading.value = false
            _isSignedOut.value = true
        }
    }
} 