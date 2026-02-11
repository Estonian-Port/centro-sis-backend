package com.estonianport.centro_sis.dto

import com.estonianport.centro_sis.model.enums.TipoPagoConcepto
import java.math.BigDecimal
import java.time.LocalDate

data class PagoDTO(
    val id: Long,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val fechaBaja: LocalDate?,
    val observaciones: String?,
    val tipo: TipoPagoConcepto,

    // Información del curso
    val cursoId: Long,
    val cursoNombre: String,

    // Información del usuario que paga (puede ser alumno o profesor)
    val usuarioPagaId: Long?,
    val usuarioPagaNombre: String?,
    val usuarioPagaApellido: String?,

    // Información del usuario que recibe (puede ser profesor o instituto)
    val usuarioRecibeId: Long?,
    val usuarioRecibeNombre: String?,
    val usuarioRecibeApellido: String?,

    // Para pagos de curso
    val inscripcionId: Long? = null,
    val retraso: Boolean? = null,
    val beneficioAplicado: Int? = null
)

data class PagoPreviewRequest(
    val inscripcionId: Long,
    val aplicarRecargo: Boolean = false
)

data class PagoPreviewDTO(
    val inscripcionId: Long,
    val alumnoNombre: String,
    val cursoNombre: String,
    val montoPorCuota: BigDecimal,
    val beneficio: Int,
    val descuento: BigDecimal,
    val recargoPorcentaje: BigDecimal,
    val recargo: BigDecimal,
    val montoFinal: BigDecimal,
    val aplicaRecargo: Boolean,
    val cuotasPagadas: Int,
    val cuotasTotales: Int,
    val cuotasEsperadas: Int,
    val cuotasAtrasadas: Int,
    val puedeRegistrar: Boolean
)

data class RegistrarPagoCursoRequest(
    val inscripcionId: Long,
    val aplicarRecargo: Boolean = false
)

data class AnularPagoDTO(
    val motivo: String
)

// Preview Alquiler
data class PagoAlquilerPreviewRequest(
    val cursoId: Long
)

data class PagoAlquilerPreviewDTO(
    val cursoId: Long,
    val cursoNombre: String,
    val profesores: List<String>, // Nombres completos
    val montoPorCuota: BigDecimal,
    val totalCuotas: Int,
    val cuotasPagadas: List<Int>, // Números de cuotas ya pagadas
    val cuotasPendientes: List<Int>, // Números de cuotas pendientes
    val puedeRegistrar: Boolean
)

// Registrar Alquiler
data class RegistrarPagoAlquilerRequest(
    val cursoId: Long,
    val numeroCuota: Int
)

// Preview Comisión
data class PagoComisionPreviewRequest(
    val cursoId: Long,
    val profesorId: Long
)

data class PagoComisionPreviewDTO(
    val cursoId: Long,
    val cursoNombre: String,
    val profesorId: Long,
    val profesorNombre: String,
    val porcentajeComision: BigDecimal,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val diasPeriodo: Int,
    val recaudacionPeriodo: BigDecimal,
    val montoComision: BigDecimal,
    val puedeRegistrar: Boolean,
    val mensajeError: String? = null
)

// Registrar Comisión
data class RegistrarPagoComisionRequest(
    val cursoId: Long,
    val profesorId: Long
)