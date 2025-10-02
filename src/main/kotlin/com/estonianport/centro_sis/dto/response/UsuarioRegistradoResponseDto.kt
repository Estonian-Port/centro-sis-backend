package com.estonianport.centro_sis.dto.response

data class UsuarioRegistradoResponseDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val celular: Long,
    val email: String,
    val estado: String,
)