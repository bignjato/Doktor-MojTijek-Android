package com.mojtijek.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DaniUnos(
    val id: String,
    val datum: String, // ISO-8601 date
    val naslov: String,
    val opis: String,
    val simptomi: List<String> = emptyList(),
    val biljeske: String? = null
)

@Serializable
data class Terapija(
    val id: String,
    val naziv: String,
    val dijagnoza: String,
    val trajanjeDana: Int,
    val aktivna: Boolean,
    val napomene: String = ""
)

@Serializable
data class UserProfile(
    val id: String,
    val ime: String,
    val prezime: String,
    val email: String,
    val notifikacije: Boolean = true,
    val reminderi: Boolean = true
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
