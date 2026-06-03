package com.example.myapplication

import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.UserData
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.ui.login.LoginState
import com.example.myapplication.ui.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: AuthRepository

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields sets error state`() = runTest {
        viewModel.login("", "password")
        assertTrue(viewModel.state is LoginState.Error)
        assertEquals("Username and password are required", (viewModel.state as LoginState.Error).message)
    }

    @Test
    fun `login success sets success state with user data`() = runTest {
        val userData = UserData(12, "checker")
        val response = LoginResponse("authorized", true, "User authorized.", userData)
        
        `when`(repository.authorize(any())).thenReturn(Result.success(response))

        viewModel.login(" checker ", "admin")
        
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state is LoginState.Success)
        assertEquals(userData, (viewModel.state as LoginState.Success).userData)
    }

    @Test
    fun `login failure sets error state with message`() = runTest {
        val errorMessage = "Invalid username or password"
        `when`(repository.authorize(any())).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.login("wrong", "wrong")
        
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state is LoginState.Error)
        assertEquals(errorMessage, (viewModel.state as LoginState.Error).message)
    }
}
