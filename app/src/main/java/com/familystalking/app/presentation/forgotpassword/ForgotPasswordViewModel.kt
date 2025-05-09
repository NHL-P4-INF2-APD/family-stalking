package com.familystalking.app.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error.asStateFlow()

    private val _resetSent = MutableStateFlow(false)
    val resetSent: StateFlow<Boolean> = _resetSent.asStateFlow()

    fun onEmailChange(email: String) {
        _email.value = email
        clearError()
    }

    fun onResetPassword() {
        if (!validateInput()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _resetSent.value = false
            
            when (val result = authenticationRepository.resetPassword(_email.value)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _resetSent.value = true
                }
                is AuthResult.Error -> {
                    _error.value = result.error
                    _isLoading.value = false
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        return when {
            _email.value.isBlank() -> {
                _error.value = AuthError.InvalidEmail
                false
            }
            else -> true
        }
    }

    private fun clearError() {
        _error.value = null
        _resetSent.value = false
    }
} 