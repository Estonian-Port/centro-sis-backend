package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.model.Horario
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object HorarioMapper {

    private val SPANISH = Locale("es", "ES")

    private val spanishToDayOfWeek = mapOf(
        "lunes" to DayOfWeek.MONDAY,
        "martes" to DayOfWeek.TUESDAY,
        "miércoles" to DayOfWeek.WEDNESDAY,
        "miercoles" to DayOfWeek.WEDNESDAY, // sin tilde
        "jueves" to DayOfWeek.THURSDAY,
        "viernes" to DayOfWeek.FRIDAY,
        "sábado" to DayOfWeek.SATURDAY,
        "sabado" to DayOfWeek.SATURDAY, // sin tilde
        "domingo" to DayOfWeek.SUNDAY
    )

    fun buildHorarioResponseDto(horario: Horario): HorarioDto {
        val diaEsp = horario.dia
            .getDisplayName(TextStyle.FULL, SPANISH)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH) else it.toString() }
        return HorarioDto(
            dia = diaEsp,
            horaInicio = horario.horaInicio.toString(),
            horaFin = horario.horaFin.toString()
        )
    }

    fun buildHorario(horarioDto: HorarioDto): Horario {
        val diaInput = horarioDto.dia.trim().lowercase(SPANISH)
        val dayOfWeek = spanishToDayOfWeek[diaInput]
            ?: try { DayOfWeek.valueOf(horarioDto.dia.uppercase()) } catch (_: Exception) { throw IllegalArgumentException("Día inválido: ${horarioDto.dia}") }

        return Horario(
            dia = dayOfWeek,
            horaInicio = java.time.LocalTime.parse(horarioDto.horaInicio),
            horaFin = java.time.LocalTime.parse(horarioDto.horaFin)
        )
    }
}