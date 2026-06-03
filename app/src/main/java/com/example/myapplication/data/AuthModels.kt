package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val status: String,
    val authorized: Boolean,
    val message: String? = null,
    val data: UserData? = null,
    val errors: List<String>? = null
)

@Serializable
data class UserData(
    val user_id: Int,
    val username: String
)

data class UserSession(
    val userId: Int,
    val username: String
)
