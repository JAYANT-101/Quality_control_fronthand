package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.UserSession
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.repository.InspectionRepository
import com.example.myapplication.session.SessionManager
import com.example.myapplication.ui.inspection.MainTabletLayout
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.login.LoginViewModel
import com.example.myapplication.ui.theme.InspectionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(RetrofitClient.authApiClient)
        val sessionManager = SessionManager(authRepository)
        val loginViewModel = LoginViewModel(authRepository, sessionManager)
        
        val apiService = RetrofitClient.apiService
        val database = AppDatabase.getDatabase(this)
        val inspectionRepository = InspectionRepository(database.apiDao(), apiService, sessionManager)

        setContent {
            InspectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    
                    LaunchedEffect(Unit) {
                        sessionManager.verifySessionBeforeAction()
                    }

                    if (sessionManager.isInitializing) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (!sessionManager.isAuthorized) {
                        LoginScreen(viewModel = loginViewModel)
                    } else {
                        val user = sessionManager.currentUser!!
                        MainTabletLayout(
                            userSession = UserSession(user.user_id, user.username),
                            inspectionRepository = inspectionRepository,
                            onLogout = {
                                scope.launch {
                                    sessionManager.logout()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
