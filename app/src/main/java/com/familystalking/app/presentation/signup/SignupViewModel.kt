package com.familystalking.app.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error.asStateFlow()

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    fun onEmailChange(email: String) {
        _email.value = email
        clearError()
    }

    fun onPasswordChange(password: String) {
        _password.value = password
        clearError()
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
        clearError()
    }

    fun onSignUp() {
        if (!validateInput()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = authenticationRepository.signUp(_email.value, _password.value)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _navigateTo.value = Screen.Home.route
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
            _password.value.length < 6 -> {
                _error.value = AuthError.WeakPassword
                false
            }
            _password.value != _confirmPassword.value -> {
                _error.value = AuthError.PasswordsDoNotMatch
                false
            }
            else -> true
        }
    }

    private fun clearError() {
        _error.value = null
    }

    fun onNavigated() {
        _navigateTo.value = null
    }
} 