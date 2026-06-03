package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.data.UserSession
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.ui.inspection.MainTabletLayout
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.login.LoginViewModel
import com.example.myapplication.ui.theme.InspectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(RetrofitClient.apiService)
        val loginViewModel = LoginViewModel(authRepository)

        setContent {
            InspectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var session by remember { mutableStateOf<UserSession?>(null) }

                    if (session == null) {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = { userData ->
                                session = UserSession(userData.user_id, userData.username)
                            }
                        )
                    } else {
                        MainTabletLayout(userSession = session!!)
                    }
                }
            }
        }
    }
}
