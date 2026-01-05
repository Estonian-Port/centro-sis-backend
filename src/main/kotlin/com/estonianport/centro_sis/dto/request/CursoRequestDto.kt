package com.estonianport.centro_sis.dto.request

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.response.TipoPagoDto

data class CursoAlquilerRequestDto (
    val id: Long,
    val nombre: String,
    val montoAlquiler: Double,
    val profesoresId : Set<Long>,
    val fechaInicio: String,
    val fechaFin: String
)

data class CursoComisionRequestDto (
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val tipoPago: Set<TipoPagoDto>,
    val recargo : Double?,
    val comisionProfesor : Double?,
    val profesoresId : Set<Long>,
    val fechaInicio: String,
    val fechaFin: String
)