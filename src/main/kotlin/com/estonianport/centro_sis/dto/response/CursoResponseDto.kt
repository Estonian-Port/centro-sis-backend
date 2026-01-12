package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto

data class CursoResponseDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: List<AlumnoResponseDto>,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val profesores: Set<UsuarioResponseDto>,
    val tiposPago: List<TipoPagoDto>,
    val inscripciones : List<InscripcionResponseDto>,
    val recargoPorAtraso: Double,
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