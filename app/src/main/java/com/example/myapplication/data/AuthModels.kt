package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val status: String,
    val authorized: Boolean,
    val message: String? = null,
    val errors: List<String>? = null,
    val data: AuthorizedUser? = null
)

@Serializable
data class AuthorizedUser(
    val user_id: Int,
    val username: String
)

data class AuthResult(
    val isSuccess: Boolean,
    val user: AuthorizedUser? = null,
    val message: String? = null
)

data class UserSession(
    val userId: Int,
    val username: String
)
