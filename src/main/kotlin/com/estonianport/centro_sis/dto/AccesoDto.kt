package com.estonianport.centro_sis.dto

import com.estonianport.centro_sis.model.enums.TipoAcceso
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccesoDTO(
    val id: Long,
    val usuarioId: Long,
    val usuarioNombre: String,
    val usuarioApellido: String,
    val usuarioDni: String,
    val fechaHora: LocalDateTime,
    val tipoAcceso: TipoAcceso,
    val alertaPagos: AlertaPagosDTO?
)

data class RegistrarAccesoDTO(
    val usuarioId: Long
)

data class RegistrarAccesoQRRequest(
    val usuarioId: Long,
)

data class AlertaPagosDTO(
    val tieneAtrasos: Boolean,
    val cursosAtrasados: List<CursoAtrasoDTO>,
    val mensaje: String?
)

data class CursoAtrasoDTO(
    val cursoNombre: String,
    val cuotasAtrasadas: Int,
    val deudaPendiente: BigDecimal
)

data class EstadisticasAccesoDTO(
    val totalHoy: Int,
    val totalEstaSemana: Int,
    val totalEsteMes: Int,
    val promedioDiario: Double,
)