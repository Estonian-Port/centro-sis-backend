package com.estonianport.centro_sis.dto.request

data class UsuarioRegistroRequestDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val celular: Long,
    val nuevoPassword: String,
    val confirmacionPassword: String,
)