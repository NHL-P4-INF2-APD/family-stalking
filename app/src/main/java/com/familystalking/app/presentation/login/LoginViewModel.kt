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

/**
 * ViewModel for managing the login screen's UI state and authentication operations.
 * 
 * This ViewModel handles user input validation, authentication requests, and navigation
 * for the login flow. It manages the state of email/password inputs, loading states,
 * error handling, and coordinates with the AuthenticationRepository for actual
 * authentication operations.
 * 
 * @property authenticationRepository Repository for handling authentication operations
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _email = MutableLiveData("")
    /**
     * Observable email input state.
     * UI components can observe this to sync with user input and display current email value.
     */
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    /**
     * Observable password input state.
     * UI components can observe this to sync with user input and display current password value.
     */
    val password: LiveData<String> = _password

    private val _isLoading = MutableLiveData(false)
    /**
     * Observable loading state indicator.
     * UI components can observe this to show/hide loading indicators during authentication operations.
     */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<AuthError?>()
    /**
     * Observable authentication error state.
     * UI components can observe this to display error messages to the user.
     * Null indicates no current error.
     */
    val error: LiveData<AuthError?> = _error

    private val _navigateTo = MutableLiveData<String?>()
    /**
     * Observable navigation trigger.
     * UI components can observe this to perform navigation when authentication succeeds.
     * Null indicates no pending navigation.
     */
    val navigateTo: LiveData<String?> = _navigateTo

    /**
     * Updates the email input state and clears any existing error.
     * 
     * This method should be called whenever the user modifies the email input field.
     * It automatically clears any authentication errors to provide a clean slate for new attempts.
     * 
     * @param email The new email value entered by the user
     */
    fun onEmailChange(email: String) {
        _email.value = email
        clearError()
    }

    /**
     * Updates the password input state and clears any existing error.
     * 
     * This method should be called whenever the user modifies the password input field.
     * It automatically clears any authentication errors to provide a clean slate for new attempts.
     * 
     * @param password The new password value entered by the user
     */
    fun onPasswordChange(password: String) {
        _password.value = password
        clearError()
    }

    /**
     * Initiates the sign-in process with the current email and password.
     * 
     * This method validates the input fields, performs the authentication request,
     * and handles the result by either navigating to the main app or displaying errors.
     * The loading state is managed automatically during the process.
     * 
     * If validation fails, appropriate error messages are displayed without making
     * a network request.
     */
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

    /**
     * Initiates the forgot password process for the current email.
     * 
     * This method validates that an email is provided, then sends a password reset
     * request to the authentication service. The user will receive instructions
     * via email to reset their password.
     * 
     * If no email is provided, an invalid email error is displayed.
     */
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

    /**
     * Validates the current email and password input values.
     * 
     * This method checks that the email field is not empty and that the password
     * meets minimum length requirements (6 characters). If validation fails,
     * appropriate error messages are set.
     * 
     * @return true if validation passes, false if there are validation errors
     */
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

    /**
     * Clears the current authentication error state.
     * 
     * This method is typically called when the user starts typing in input fields
     * to provide a clean slate for new authentication attempts.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Acknowledges that navigation has been handled by the UI.
     * 
     * This method should be called by UI components after they have processed
     * a navigation event to prevent duplicate navigation triggers.
     */
    fun onNavigated() {
        _navigateTo.value = null
    }
}
