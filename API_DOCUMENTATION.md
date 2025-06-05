# API Documentation

This document provides comprehensive documentation for the key APIs, interfaces, and classes in the Family Stalking Android application.

## 📚 Table of Contents

- [Domain Models](#-domain-models)
- [Repository Interfaces](#-repository-interfaces)
- [ViewModels](#-viewmodels)
- [Error Handling](#-error-handling)
- [Usage Examples](#-usage-examples)

## 🏗️ Domain Models

### AuthResult

A sealed class representing the outcome of authentication operations.

```kotlin
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val error: AuthError) : AuthResult()
}
```

**Usage:**
```kotlin
when (val result = authRepository.signIn(email, password)) {
    is AuthResult.Success -> {
        // Handle successful authentication
        navigateToHome()
    }
    is AuthResult.Error -> {
        // Handle authentication error
        showError(result.error.message)
    }
}
```

### AuthError

Enumeration of authentication error types with descriptive messages.

```kotlin
sealed class AuthError(val message: String) {
    object InvalidEmail : AuthError("Invalid email address")
    object WeakPassword : AuthError("Password must be at least 6 characters")
    object PasswordsDoNotMatch : AuthError("Passwords do not match")
    object EmailAlreadyInUse : AuthError("Email is already in use")
    object InvalidCredentials : AuthError("Invalid email or password")
    object NetworkError : AuthError("Network error occurred")
    object UnknownError : AuthError("An unknown error occurred")
}
```

### SessionState

Represents the current authentication state of the user session.

```kotlin
sealed class SessionState {
    data object Loading : SessionState()
    data object Authenticated : SessionState()
    data object Unauthenticated : SessionState()
}
```

### Location

Data class representing geographical location with timestamp.

```kotlin
data class Location(
    val id: UUID,
    val userId: UUID,
    val latitude: Double,  // Range: -90.0 to 90.0
    val longitude: Double, // Range: -180.0 to 180.0
    val timestamp: Instant
)
```

### FamilyMemberLocation

Combines user identity with location data for family member tracking.

```kotlin
data class FamilyMemberLocation(
    val userId: UUID,
    val name: String,
    val location: Location
)
```

## 🔄 Repository Interfaces

### AuthenticationRepository

Handles all authentication-related operations.

#### Properties

```kotlin
val sessionState: StateFlow<SessionState>
```
Observable session state that emits authentication state changes.

#### Methods

##### signIn
```kotlin
suspend fun signIn(email: String, password: String): AuthResult
```
Authenticates user with email and password credentials.

**Parameters:**
- `email`: User's email address
- `password`: User's password

**Returns:** `AuthResult.Success` or `AuthResult.Error`

**Example:**
```kotlin
val result = authRepository.signIn("user@example.com", "password123")
```

##### signUp
```kotlin
suspend fun signUp(email: String, password: String): AuthResult
```
Creates a new user account.

**Parameters:**
- `email`: Email for new account
- `password`: Password for new account

**Returns:** `AuthResult.Success` or `AuthResult.Error`

##### resetPassword
```kotlin
suspend fun resetPassword(email: String): AuthResult
```
Initiates password reset process.

**Parameters:**
- `email`: Email address for password reset

**Returns:** `AuthResult.Success` or `AuthResult.Error`

##### signOut
```kotlin
suspend fun signOut(): AuthResult
```
Signs out current user and invalidates session.

**Returns:** `AuthResult.Success` or `AuthResult.Error`

##### checkSession
```kotlin
suspend fun checkSession()
```
Validates current session and updates session state.

### LocationRepository

Manages location tracking and family member location sharing.

#### Methods

##### updateUserLocation
```kotlin
suspend fun updateUserLocation(latitude: Double, longitude: Double)
```
Updates current user's location.

**Parameters:**
- `latitude`: Latitude coordinate (-90.0 to 90.0)
- `longitude`: Longitude coordinate (-180.0 to 180.0)

**Throws:** `IllegalStateException` if no user is authenticated

**Example:**
```kotlin
locationRepository.updateUserLocation(52.3676, 4.9041) // Amsterdam coordinates
```

##### getUserLocation
```kotlin
suspend fun getUserLocation(userId: UUID): Location?
```
Retrieves the most recent location for a specific user.

**Parameters:**
- `userId`: Unique identifier of the user

**Returns:** Most recent `Location` or `null` if no data exists

##### getFamilyMembersLocations
```kotlin
fun getFamilyMembersLocations(): Flow<List<FamilyMemberLocation>>
```
Provides reactive stream of all family members' locations.

**Returns:** `Flow` emitting lists of `FamilyMemberLocation`

**Example:**
```kotlin
locationRepository.getFamilyMembersLocations().collect { locations ->
    updateMapMarkers(locations)
}
```

##### getAuthenticatedUserId
```kotlin
suspend fun getAuthenticatedUserId(): UUID?
```
Gets the current authenticated user's ID.

**Returns:** User ID or `null` if not authenticated

### FamilyRepository

Manages family member relationships and friendship requests.

#### Methods

##### getFamilyMembers
```kotlin
suspend fun getFamilyMembers(): List<FamilyMember>
```
Retrieves all family members for current user.

**Returns:** List of connected family members

##### getCurrentUser
```kotlin
suspend fun getCurrentUser(): FamilyMember
```
Gets current user's family member information.

**Returns:** Current user as `FamilyMember`

##### sendFriendshipRequest
```kotlin
suspend fun sendFriendshipRequest(receiverId: String): Result<Unit>
```
Sends friendship request to another user.

**Parameters:**
- `receiverId`: Target user's unique identifier

**Returns:** `Result<Unit>` indicating success or failure

##### acceptFriendshipRequest
```kotlin
suspend fun acceptFriendshipRequest(requestId: String): Result<Unit>
```
Accepts a pending friendship request.

**Parameters:**
- `requestId`: Unique identifier of the request

**Returns:** `Result<Unit>` indicating success or failure

##### rejectFriendshipRequest
```kotlin
suspend fun rejectFriendshipRequest(requestId: String): Result<Unit>
```
Rejects a pending friendship request.

**Parameters:**
- `requestId`: Unique identifier of the request

**Returns:** `Result<Unit>` indicating success or failure

##### getPendingFriendshipRequests
```kotlin
suspend fun getPendingFriendshipRequests(): List<FriendshipRequest>
```
Gets all pending friendship requests for current user.

**Returns:** List of pending requests

## 🎯 ViewModels

### LoginViewModel

Manages login screen state and authentication operations.

#### Observable Properties

```kotlin
val email: LiveData<String>
val password: LiveData<String>
val isLoading: LiveData<Boolean>
val error: LiveData<AuthError?>
val navigateTo: LiveData<String?>
```

#### Methods

##### onEmailChange
```kotlin
fun onEmailChange(email: String)
```
Updates email input state and clears errors.

##### onPasswordChange
```kotlin
fun onPasswordChange(password: String)
```
Updates password input state and clears errors.

##### onSignIn
```kotlin
fun onSignIn()
```
Initiates sign-in process with current email/password.

##### onForgotPassword
```kotlin
fun onForgotPassword()
```
Initiates password reset for current email.

##### clearError
```kotlin
fun clearError()
```
Clears current authentication error.

##### onNavigated
```kotlin
fun onNavigated()
```
Acknowledges navigation event has been handled.

## ⚠️ Error Handling

### Error Types

All repository methods use either `AuthResult` for authentication operations or standard `Result<T>` for other operations.

### Error Handling Patterns

#### Authentication Errors
```kotlin
when (val result = authRepository.signIn(email, password)) {
    is AuthResult.Success -> {
        // Handle success
    }
    is AuthResult.Error -> when (result.error) {
        is AuthError.InvalidCredentials -> showInvalidCredentialsMessage()
        is AuthError.NetworkError -> showNetworkErrorMessage()
        is AuthError.InvalidEmail -> highlightEmailField()
        else -> showGenericErrorMessage()
    }
}
```

#### Repository Errors
```kotlin
val result = familyRepository.sendFriendshipRequest(userId)
result.fold(
    onSuccess = { showSuccessMessage() },
    onFailure = { error -> showErrorMessage(error.message) }
)
```

### Exception Handling

#### Location Operations
```kotlin
try {
    locationRepository.updateUserLocation(lat, lng)
} catch (e: IllegalStateException) {
    // User not authenticated
    redirectToLogin()
} catch (e: Exception) {
    // Other errors
    showErrorMessage("Location update failed")
}
```

## 💡 Usage Examples

### Complete Authentication Flow

```kotlin
class AuthenticationExample {
    
    suspend fun completeLoginFlow(email: String, password: String) {
        // Start loading
        setLoadingState(true)
        
        try {
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    // Monitor session state
                    authRepository.sessionState.collect { state ->
                        when (state) {
                            is SessionState.Authenticated -> navigateToHome()
                            is SessionState.Unauthenticated -> navigateToLogin()
                            is SessionState.Loading -> showLoadingIndicator()
                        }
                    }
                }
                is AuthResult.Error -> {
                    handleAuthError(result.error)
                }
            }
        } finally {
            setLoadingState(false)
        }
    }
    
    private fun handleAuthError(error: AuthError) {
        when (error) {
            is AuthError.InvalidCredentials -> {
                showErrorMessage("Please check your email and password")
                clearPasswordField()
            }
            is AuthError.NetworkError -> {
                showRetryDialog("Network connection failed")
            }
            else -> showErrorMessage(error.message)
        }
    }
}
```

### Location Tracking Setup

```kotlin
class LocationTrackingExample {
    
    fun setupLocationTracking() {
        // Start observing family locations
        lifecycleScope.launch {
            locationRepository.getFamilyMembersLocations().collect { locations ->
                updateMapWithLocations(locations)
            }
        }
        
        // Update user's location periodically
        locationUpdateJob = lifecycleScope.launch {
            while (isActive) {
                getCurrentLocation()?.let { location ->
                    locationRepository.updateUserLocation(
                        location.latitude,
                        location.longitude
                    )
                }
                delay(30_000) // Update every 30 seconds
            }
        }
    }
    
    private fun updateMapWithLocations(locations: List<FamilyMemberLocation>) {
        mapView.clear()
        locations.forEach { memberLocation ->
            val marker = createMarker(
                position = LatLng(
                    memberLocation.location.latitude,
                    memberLocation.location.longitude
                ),
                title = memberLocation.name,
                snippet = "Last updated: ${memberLocation.location.timestamp}"
            )
            mapView.addMarker(marker)
        }
    }
}
```

### Family Management

```kotlin
class FamilyManagementExample {
    
    suspend fun setupFamilyManagement() {
        // Load family members
        val familyMembers = familyRepository.getFamilyMembers()
        displayFamilyMembers(familyMembers)
        
        // Load pending requests
        val pendingRequests = familyRepository.getPendingFriendshipRequests()
        displayPendingRequests(pendingRequests)
    }
    
    suspend fun handleFriendshipRequest(requestId: String, accept: Boolean) {
        val result = if (accept) {
            familyRepository.acceptFriendshipRequest(requestId)
        } else {
            familyRepository.rejectFriendshipRequest(requestId)
        }
        
        result.fold(
            onSuccess = {
                showSuccessMessage(if (accept) "Request accepted" else "Request rejected")
                refreshFamilyList()
            },
            onFailure = { error ->
                showErrorMessage("Failed to process request: ${error.message}")
            }
        )
    }
}
```

## 🔗 Related Documentation

- [Architecture Documentation](ARCHITECTURE.md) - Detailed architecture overview
- [README.md](README.md) - Project setup and general information
- [ERD.md](ERD.md) - Database schema documentation