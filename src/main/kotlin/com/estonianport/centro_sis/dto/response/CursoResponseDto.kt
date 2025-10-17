package com.estonianport.centro_sis.dto.response

import java.time.DayOfWeek
import java.time.LocalTime

data class CursoResponseDto (
    val id: Long,
    val nombre: String,
    val dias: MutableList<DayOfWeek>,
    val horarios: Set<LocalTime>,
    val arancel: Double
)