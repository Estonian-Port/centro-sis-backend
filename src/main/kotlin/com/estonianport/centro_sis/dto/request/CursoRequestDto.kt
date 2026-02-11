package com.estonianport.centro_sis.dto.request

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.response.TipoPagoDto

data class CursoAlquilerAdminRequestDto(
    val id: Long,
    val nombre: String,
    val montoAlquiler: Double,
    val cuotasAlquiler: Int,
    val profesoresId: List<Long>,
    val fechaInicio: String,
    val fechaFin: String,
)

data class CursoAlquilerProfeRequestDto(
    val id: Long,
    val horarios: List<HorarioDto>,
    val tiposPago: List<TipoPagoDto>,
    val recargo: Double
)

data class CursoComisionRequestDto(
    val id: Long,
    val nombre: String,
    val horarios: List<HorarioDto>,
    val tipoPago: List<TipoPagoDto>,
    val recargo: Double?,
    val comisionProfesor: Double?,
    val profesoresId: List<Long>,
    val fechaInicio: String,
    val fechaFin: String
)