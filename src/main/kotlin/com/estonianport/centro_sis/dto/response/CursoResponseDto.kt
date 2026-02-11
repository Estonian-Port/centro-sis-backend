package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto

data class CursoResponseDto(
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: List<AlumnoResponseDto>,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val estadoAlta: String,
    val profesores: Set<UsuarioResponseDto>,
    val tiposPago: List<TipoPagoDto>,
    val inscripciones: List<InscripcionResponseDto>,
    val recargoPorAtraso: Double,
    val tipoCurso: String,
    val montoAlquiler: Double?,
    val cuotasAlquiler: Int?,
)

data class CursoAlumnoResponseDto(
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: List<AlumnoResponseDto>,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val estadoAlta: String,
    val profesores: Set<UsuarioResponseDto>,
    val tiposPago: List<TipoPagoDto>,
    val inscripciones: List<InscripcionResponseDto>,
    val recargoPorAtraso: Double,
    val tipoCurso: String,
    val estadoPago: String,
    val tipoPagoElegido: TipoPagoDto,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val beneficio: Int,
    val pagosRealizados: List<PagoResponseDto>,
    val puntos: Int,
)