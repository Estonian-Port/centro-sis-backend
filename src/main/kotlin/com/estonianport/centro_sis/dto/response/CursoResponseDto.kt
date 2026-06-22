package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.ProfesorListaResponseDto
import com.estonianport.centro_sis.dto.request.AdultoResponsableDto
import java.io.Serializable

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
): Serializable

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
): Serializable

data class CursoResumenDto(
    val id: Long,
    val nombre: String,
    val tipoCurso: String,
    val estado: String,
    val estadoAlta: String,
    val fechaInicio: String,
    val fechaFin: String,
    val horarios: List<HorarioDto>,
    val profesores: List<ProfesorResumenDto>,
    val cantidadAlumnosInscriptos: Int
) : Serializable

data class ProfesorResumenDto(
    val id: Long,
    val nombreCompleto: String
) : Serializable

data class CursoDetalleDto(
    val id: Long,
    val nombre: String,
    val tipoCurso: String,
    val estado: String,
    val estadoAlta: String,
    val fechaInicio: String,
    val fechaFin: String,
    val recargoPorAtrasoPorcentaje: Double,
    val horarios: Set<HorarioDto>,
    val tiposPago: List<TipoPagoDto>,
    val profesores: List<ProfesorResumenDto>,
    val montoAlquiler: Double?,
    val cuotasAlquiler: Int?,
    val totalAlumnosInscriptos: Int
) : Serializable

data class CursoAlumnoInscriptoDto(
    val id: Long,
    val inscripcionId: Long,
    val nombreCompleto: String,
    val email : String,
    val dni : String,
    val celular : String,
    val fechaNacimiento : String,
    val estadoPago: String,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val puntos: Int,
    val adultoResponsable: AdultoResponsableDto?
) : Serializable

data class MiInscripcionCursoDto(
    val id: Long,
    val nombreCurso: String,
    val estado: String,
    val estadoPago: String,
    val tipoPagoElegido: TipoPagoDto,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val puntos: Int,
    val beneficio: Int,
) : Serializable

data class CursoConteoAlumnosDto(
    val id: Long,
    val cantidad: Long
): Serializable

data class InscripcionAlumnoSummaryDto(
    val id: Long,
    val nombre: String,
    val estado: String,
    val estadoAlta: String,
    val estadoPago: String,
    val fechaInicio: String,
    val fechaFin: String,
    val fechaInscripcion: String,
    val horarios: Set<HorarioDto>,
    val profesores: List<ProfesorResumenDto>,
    val tipoPagoElegido: TipoPagoDto,
    val porcentajeAsistencia: Double,
    val tipoCurso: String,
    val puntos: Int,
): Serializable

data class CursoProfesorSummaryDto(
    val id: Long,
    val nombre: String,
    val estado: String,
    val estadoAlta: String,
    val fechaInicio: String,
    val fechaFin: String,
    val horarios: Set<HorarioDto>,
    val profesores: List<ProfesorResumenDto>,
    val totalAlumnosInscriptos: Int,
    val tipoCurso: String,
): Serializable