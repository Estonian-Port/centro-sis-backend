package com.estonianport.centro_sis.dto.request

import com.estonianport.centro_sis.model.AdultoResponsable
import java.time.LocalDate

data class UsuarioRegistroRequestDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val celular: Long,
    val fechaNacimiento: LocalDate,
    val password: String,
    val adultoResponsable: AdultoResponsable? = null
)