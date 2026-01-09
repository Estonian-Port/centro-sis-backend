package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.InscripcionResponseDto
import com.estonianport.centro_sis.model.Inscripcion

object InscripcionMapper {

    fun buildInscripcionResponseDto(inscripcion : Inscripcion) : InscripcionResponseDto {
        return InscripcionResponseDto(
            id = inscripcion.id,
            alumno = UsuarioMapper.buildAlumno(inscripcion.alumno.usuario),
            tipoPagoElegido = TipoPagoMapper.buildTipoPagoResponseDto(inscripcion.tipoPagoSeleccionado),
            fechaInscripcion = inscripcion.fechaInscripcion.toString(),
            pagosRealizados = inscripcion.pagos.map { PagoMapper.buildPagoResponseDto(it) },
            estadoPago = inscripcion.estadoPago.name,
            beneficio = inscripcion.beneficio,
            puntos = inscripcion.puntos,
            porcentajeAsistencia = 0.0, //inscripcion.porcentajeAsistencia,
            estado = inscripcion.estado.name
        )
    }
}