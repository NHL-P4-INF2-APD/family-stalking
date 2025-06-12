package com.familystalking.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.SessionState
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class MainViewModel @Inject constructor(
    authenticationRepository: AuthenticationRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authenticationRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SessionState.Loading
        )

    init {
        Log.d("MainViewModel", "MainViewModel initialized. Session state will be collected from repository.")
    }
}