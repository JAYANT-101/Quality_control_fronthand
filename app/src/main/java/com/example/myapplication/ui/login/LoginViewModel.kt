package com.example.myapplication.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.session.SessionManager
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var state by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    var usernameInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")

    fun login() {
        val username = usernameInput.trim()

        if (username.isEmpty() || passwordInput.isEmpty()) {
            state = LoginState.Error("Username and password are required")
            return
        }

        viewModelScope.launch {
            state = LoginState.Loading
            val result = authRepository.login(username, passwordInput)
            if (result.isSuccess && result.user != null) {
                // Set flag to skip the next session verification network call
                // because we just got a successful login and cookie is being set.
                sessionManager.skipVerifyNextAction = true

                sessionManager.currentUser = result.user
                sessionManager.isAuthorized = true
                state = LoginState.Success
            } else {
                state = LoginState.Error(result.message ?: "An unexpected error occurred")
                sessionManager.currentUser = null
                sessionManager.isAuthorized = false
            }
        }
    }

    fun resetError() {
        if (state is LoginState.Error) {
            state = LoginState.Idle
        }
    }
}
