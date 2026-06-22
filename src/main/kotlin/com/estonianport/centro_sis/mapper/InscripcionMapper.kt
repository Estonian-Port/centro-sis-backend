package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.InscripcionAlumnoSummaryDto
import com.estonianport.centro_sis.dto.response.InscripcionResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper.profesoresResumen
import com.estonianport.centro_sis.model.Inscripcion

object InscripcionMapper {

    fun buildInscripcionResponseDto(inscripcion: Inscripcion): InscripcionResponseDto {
        return InscripcionResponseDto(
            id = inscripcion.id,
            alumno = UsuarioMapper.buildAlumno(inscripcion.alumno.usuario),
            tipoPagoElegido = TipoPagoMapper.buildTipoPagoResponseDto(inscripcion.tipoPagoSeleccionado),
            fechaInscripcion = inscripcion.fechaInscripcion.toString(),
            pagosRealizados = inscripcion.pagos.map { PagoMapper.buildPagoResponseDto(it) },
            estadoPago = inscripcion.estadoPago.name,
            beneficio = inscripcion.beneficio,
            puntos = inscripcion.puntos,
            porcentajeAsistencia = inscripcion.curso.getPorcentajeAsistenciaAlumno(inscripcion.alumno.id),
            estado = inscripcion.estado.name
        )
    }

    fun buildInscripcionAlumnoSummaryDto(inscripcion: Inscripcion): InscripcionAlumnoSummaryDto {
        val curso = inscripcion.curso
        return InscripcionAlumnoSummaryDto(
            id = curso.id,
            nombre = curso.nombre,
            estado = curso.estado.name,
            estadoAlta = curso.estadoAlta.name,
            estadoPago = inscripcion.estadoPago.name,
            fechaInicio = curso.fechaInicio.toString(),
            fechaFin = curso.fechaFin.toString(),
            fechaInscripcion = inscripcion.fechaInscripcion.toString(),
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            profesores = curso.profesoresResumen(),
            tipoPagoElegido = TipoPagoMapper.buildTipoPagoResponseDto(inscripcion.tipoPagoSeleccionado),
            porcentajeAsistencia = curso.getPorcentajeAsistenciaAlumno(inscripcion.alumno.id),
            tipoCurso = curso.tipoCurso.name,
            puntos = inscripcion.puntos,
        )
    }
}