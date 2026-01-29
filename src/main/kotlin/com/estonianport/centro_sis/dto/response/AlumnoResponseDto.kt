package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.request.AdultoResponsableDto

data class AlumnoResponseDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    val celular: String,
    val fechaNacimiento: String,
    val adultoResponsable : AdultoResponsableDto?
)
