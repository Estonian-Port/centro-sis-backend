package com.estonianport.centro_sis.dto.request

import com.estonianport.centro_sis.dto.HorarioDto

data class CursoRequestDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val arancel: Double,
    val tipoPago: Set<String>,
    val profesoresId : Set<Long>
)