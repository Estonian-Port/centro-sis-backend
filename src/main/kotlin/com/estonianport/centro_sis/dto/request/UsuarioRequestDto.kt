package com.estonianport.centro_sis.dto.request

data class UsuarioRequestDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val celular: Long,
    val email: String,
    val password: String,
    val fechaDeNacimiento: String,
)

data class UsuarioAltaRequestDto(
    val email: String,
    val roles: List<String>
)

data class UsuarioCambioPasswordRequestDto(
    val passwordActual: String,
    val nuevoPassword: String,
    val confirmacionPassword: String
)

data class UsuarioUpdatePerfilRequestDto(
    val nombre: String,
    val apellido: String,
    val dni: String,
    val celular: Long,
    val email: String
)