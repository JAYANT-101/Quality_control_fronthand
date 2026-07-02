package com.example.myapplication

import com.example.myapplication.data.AuthResult
import com.example.myapplication.data.AuthorizedUser
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.session.SessionManager
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
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: AuthRepository

    @Mock
    private lateinit var sessionManager: SessionManager

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(repository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields sets error state`() = runTest {
        viewModel.usernameInput = ""
        viewModel.passwordInput = "password"
        viewModel.login()
        assertTrue(viewModel.state is LoginState.Error)
        assertEquals("Username and password are required", (viewModel.state as LoginState.Error).message)
    }

    @Test
    fun `login success sets success state`() = runTest {
        val user = AuthorizedUser(12, "checker")
        val result = AuthResult(isSuccess = true, user = user)
        
        `when`(repository.login(any(), any())).thenReturn(result)

        viewModel.usernameInput = " checker "
        viewModel.passwordInput = "admin"
        viewModel.login()
        
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state is LoginState.Success)
        verify(sessionManager).currentUser = user
        verify(sessionManager).isAuthorized = true
    }

    @Test
    fun `login failure sets error state with message`() = runTest {
        val errorMessage = "Invalid username or password"
        val result = AuthResult(isSuccess = false, message = errorMessage)
        `when`(repository.login(any(), any())).thenReturn(result)

        viewModel.usernameInput = "wrong"
        viewModel.passwordInput = "wrong"
        viewModel.login()
        
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state is LoginState.Error)
        assertEquals(errorMessage, (viewModel.state as LoginState.Error).message)
    }
}
