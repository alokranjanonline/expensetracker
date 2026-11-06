package com.sedexpensetracker.expensetracker.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val registrationSuccess: Boolean = false,
    val userDisplayName: String = "",
    val userEmail: String = ""
)

class AuthViewModel(
    context: Context,
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private fun formatErrorMessage(rawError: String?): String {
        val err = rawError?.lowercase() ?: ""
        if (err.contains("network") || err.contains("connection") || err.contains("offline") ||
            err.contains("unreachable") || err.contains("interrupted") || err.contains("host") || err.contains("a network error")) {
            return "No active internet connection. Please connect to the internet and try again."
        }
        return rawError ?: "An unexpected error occurred. Please try again."
    }

    fun checkAuthStatus() {
        val user = authRepository.currentUser
        val isVerified = authRepository.isUserLoggedInAndVerified
        if (user != null && isVerified) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                userDisplayName = user.displayName ?: "User",
                userEmail = user.email ?: ""
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = false,
                userDisplayName = "",
                userEmail = ""
            )
        }
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName, errorMessage = null)
    }

    fun onLastNameChange(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName, errorMessage = null)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, errorMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter email and password.")
            return
        }

        if (!isOnline()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No active internet connection. Please connect to the internet and try again."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            result.onSuccess { user ->
                val name = user.displayName ?: "User"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userDisplayName = name,
                    userEmail = user.email ?: "",
                    infoMessage = "Login successful! Welcome back, $name.",
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(error.localizedMessage)
                )
            }
        }
    }

    fun register() {
        val firstName = _uiState.value.firstName.trim()
        val lastName = _uiState.value.lastName.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields.")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords do not match.")
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }

        if (!isOnline()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No active internet connection. Please connect to the internet and try again."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.registerUser(firstName, lastName, email, password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationSuccess = true,
                    infoMessage = "Registration successful! A verification link has been sent to $email. Please verify before logging in.",
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(error.localizedMessage)
                )
            }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your registered email.")
            return
        }

        if (!isOnline()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No active internet connection. Please connect to the internet and try again."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    infoMessage = "Password reset link sent to $email. Please check your inbox."
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(error.localizedMessage)
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }
}
