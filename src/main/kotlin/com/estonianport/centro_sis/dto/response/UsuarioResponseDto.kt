package com.estonianport.centro_sis.dto.response

data class UsuarioResponseDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val email: String,
    val celular: Long,
    val isAdmin: Boolean,
    val primerLogin: Boolean
)