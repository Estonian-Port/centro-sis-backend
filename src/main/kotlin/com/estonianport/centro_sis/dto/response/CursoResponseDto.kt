package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto

data class CursoResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val tiposPago: Set<TipoPagoResponseDto>,
    val profesores: Set<String>,
)

data class CursoAlumnoResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val arancel: Double,
    val tipoPagoElegido: String,
    val profesores: Set<String>,
    val beneficio: Int,
    val estadoPago: String,
)

data class CursoProfesorResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: Int,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
)