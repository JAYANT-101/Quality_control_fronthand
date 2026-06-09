package com.example.myapplication.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.UserData
import com.example.myapplication.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userData: UserData) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    var state by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    fun login(usernameInput: String, passwordInput: String) {
        val username = usernameInput.trim()

        if (username.isEmpty() || passwordInput.isEmpty()) {
            state = LoginState.Error("Username and password are required")
            return
        }

        viewModelScope.launch {
            state = LoginState.Loading
            repository.authorize(LoginRequest(username, passwordInput))
                .onSuccess { response ->
                    state = if (response.data != null) {
                        LoginState.Success(response.data)
                    } else {
                        LoginState.Error("Invalid server response")
                    }
                }
                .onFailure { error ->
                    state = LoginState.Error(error.message ?: "An unexpected error occurred")
                }
        }
    }

    fun resetError() {
        if (state is LoginState.Error) {
            state = LoginState.Idle
        }
    }
}
