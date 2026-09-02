package com.mojtijek.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class User(
    val id: String,
    val name: String,
    val email: String
)

class UserRepository(private val apiClient: ApiClient) {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: Flow<List<User>> = _users.asStateFlow()
}
