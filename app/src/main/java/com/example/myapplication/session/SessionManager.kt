package com.example.myapplication.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.myapplication.data.AuthorizedUser
import com.example.myapplication.repository.AuthRepository

class SessionManager(private val authRepository: AuthRepository) {
    var currentUser by mutableStateOf<AuthorizedUser?>(null)
    var isAuthorized by mutableStateOf(false)
    var isInitializing by mutableStateOf(true)
    
    // Flag to skip the next network verification if we just logged in successfully
    var skipVerifyNextAction = false

    suspend fun verifySessionBeforeAction(): Boolean {
        if (isAuthorized) {
            isInitializing = false
            return true
        }
        
        val result = authRepository.verifySession()
        return if (result.isSuccess && result.user != null) {
            currentUser = result.user
            isAuthorized = true
            isInitializing = false
            true
        } else {
            currentUser = null
            isAuthorized = false
            isInitializing = false
            false
        }
    }

    suspend fun logout(): Boolean {
        val result = authRepository.logout()
        currentUser = null
        isAuthorized = false
        skipVerifyNextAction = false
        return result.isSuccess
    }
}
