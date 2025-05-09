package com.familystalking.app.domain.model

sealed class SessionState {
    data object Loading : SessionState()
    data object Authenticated : SessionState()
    data object Unauthenticated : SessionState()
} 