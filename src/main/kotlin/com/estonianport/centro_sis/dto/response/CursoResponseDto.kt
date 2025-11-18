package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto
import java.time.DayOfWeek
import java.time.LocalTime

data class CursoResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val arancel: Double,
    val tiposPago: Set<String>,
    val profesores: Set<String>,
)

data class CursoAlumnoResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val arancel: Double,
    val tiposPago: Set<String>,
    val profesores: Set<String>,
    val beneficios: Set<String>,
    val estadoPago: String,
)

data class CursoProfesorResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: Int,
    val fechaIncio: String,
    val fechaFin: String,
    val estado: String,
)