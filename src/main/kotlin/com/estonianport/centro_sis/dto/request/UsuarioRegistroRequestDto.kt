package com.estonianport.centro_sis.dto.request

import java.time.LocalDate

data class UsuarioRegistroRequestDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val celular: Long,
    val fechaNacimiento: LocalDate,
    val password: String,
    val adultoResponsable: AdultoResponsableDto? = null,
)

data class AdultoResponsableDto(
    val nombre: String,
    val apellido: String,
    val dni: String,
    val celular: Long,
    val relacion: String
)