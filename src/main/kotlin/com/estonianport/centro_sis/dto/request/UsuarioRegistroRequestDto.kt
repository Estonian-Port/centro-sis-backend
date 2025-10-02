package com.estonianport.unique.dto.request

data class UsuarioRegistroRequestDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val celular: Long,
    val nuevoPassword: String,
    val confirmacionPassword: String,
)