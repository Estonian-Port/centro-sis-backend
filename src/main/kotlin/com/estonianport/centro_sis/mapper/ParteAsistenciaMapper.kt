package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.ParteAsistenciaResponseDto
import com.estonianport.centro_sis.model.ParteAsistencia

object ParteAsistenciaMapper {
    fun buildParteAsistenciaResponseDto(parte : ParteAsistencia): ParteAsistenciaResponseDto {
        return ParteAsistenciaResponseDto(
            id = parte.id,
            fecha = parte.fecha.toString(),
            totalAlumnos = parte.getTotalAlumnos(),
            totalPresentes = parte.getTotalPresentes(),
            totalAusentes = parte.getTotalAusentes(),
            presentes = parte.getAlumnosPresentes().map { UsuarioMapper.buildUsuarioSimpleDto(it) },
            ausentes = parte.getAlumnosAusentes().map { UsuarioMapper.buildUsuarioSimpleDto(it) },
            porcentajeAsistencia = parte.getPorcentajeAsistencia(),
            tomadoPor = UsuarioMapper.buildUsuarioSimpleDto(parte.tomadoPor)
        )
    }

}