package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.InscripcionResponseDto
import com.estonianport.centro_sis.model.Inscripcion

object InscripcionMapper {

    fun buildInscripcionResponseDto(inscripcion : Inscripcion) : InscripcionResponseDto {
        return InscripcionResponseDto(
            id = inscripcion.id,
            nombreAlumno = inscripcion.alumno.usuario.nombreCompleto(),
            nombreCurso = inscripcion.curso.nombre,
            tipoPagoSeleccionado = inscripcion.tipoPagoSeleccionado.tipoPago.name,
            beneficio = inscripcion.beneficio,
            fechaInscripcion = inscripcion.fechaInscripcion.toString(),
            estadoPago = inscripcion.estadoPago.name
        )
    }
}