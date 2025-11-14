package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.model.Horario

object HorarioMapper {

    fun buildHorarioResponseDto(horario: Horario): HorarioDto {
        return HorarioDto(
            dia = horario.dia.name,
            horaInicio = horario.horaInicio.toString(),
            horaFin = horario.horaFin.toString()
        )
    }

    fun buildHorario(horarioDto: HorarioDto): Horario {
        return Horario(
            dia = java.time.DayOfWeek.valueOf(horarioDto.dia),
            horaInicio = java.time.LocalTime.parse(horarioDto.horaInicio),
            horaFin = java.time.LocalTime.parse(horarioDto.horaFin)
        )
    }
}