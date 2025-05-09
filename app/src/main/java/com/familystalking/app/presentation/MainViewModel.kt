package com.familystalking.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authenticationRepository.sessionState

    fun checkSession() {
        viewModelScope.launch {
            authenticationRepository.checkSession()
        }
    }
} 