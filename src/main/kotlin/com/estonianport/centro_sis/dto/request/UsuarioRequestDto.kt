package com.estonianport.centro_sis.dto.request

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
    val rol: String
)

data class UsuarioCambioPasswordRequestDto (
    val email: String,
    val passwordActual: String,
    val nuevoPassword: String,
    val confirmacionPassword: String
)