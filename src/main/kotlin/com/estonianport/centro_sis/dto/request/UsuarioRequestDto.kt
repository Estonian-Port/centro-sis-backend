package com.estonianport.unique.dto.request

data class UsuarioRequestDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val celular: Long,
    val email: String,
    val password: String
)

data class UsuarioAltaRequestDto (
    val email: String,
)

data class UsuarioCambioPasswordRequestDto (
    val email: String,
    val passwordActual: String,
    val nuevoPassword: String,
    val confirmacionPassword: String
)