package com.familystalking.app.presentation.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.AuthError
import com.familystalking.app.domain.model.AuthResult
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<AuthError?>()
    val error: LiveData<AuthError?> = _error

    private val _navigateTo = MutableLiveData<String?>()
    val navigateTo: LiveData<String?> = _navigateTo

    fun onEmailChange(email: String) {
        _email.value = email
        clearError()
    }

    fun onPasswordChange(password: String) {
        _password.value = password
        clearError()
    }

    fun onSignIn() {
        if (!validateInput()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authenticationRepository.signIn(_email.value ?: "", _password.value ?: "")) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _navigateTo.value = Screen.Map.route
                }
                is AuthResult.Error -> {
                    _error.value = result.error
                    _isLoading.value = false
                }
            }
        }
    }

    fun onForgotPassword() {
        if (_email.value.isNullOrBlank()) {
            _error.value = AuthError.InvalidEmail
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authenticationRepository.resetPassword(_email.value!!)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    // Show success message (handled by caller)
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
            _email.value.isNullOrBlank() -> {
                _error.value = AuthError.InvalidEmail
                false
            }
            (_password.value?.length ?: 0) < 6 -> {
                _error.value = AuthError.WeakPassword
                false
            }
            else -> true
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onNavigated() {
        _navigateTo.value = null
    }
}
