package com.estonianport.centro_sis.dto

import java.math.BigDecimal
import java.time.LocalDate

// ============================================
// DTO PRINCIPAL: Reporte Financiero Mensual
// ============================================

data class ReporteFinancieroMensualDTO(
    val mes: Int,
    val anio: Int,
    val resumen: ResumenFinancieroDTO,
    val detalleIngresos: DetalleIngresosDTO,
    val detalleEgresos: DetalleEgresosDTO,
    val movimientos: List<MovimientoFinancieroDTO>,
    val comparacionMesAnterior: ComparacionMensualDTO?
)

// ============================================
// RESUMEN (Cards superiores)
// ============================================

data class ResumenFinancieroDTO(
    val totalIngresos: BigDecimal,
    val totalEgresos: BigDecimal,
    val balance: BigDecimal,
    val porcentajeCambioIngresos: BigDecimal?, // Vs mes anterior
    val porcentajeCambioEgresos: BigDecimal?,
    val porcentajeCambioBalance: BigDecimal?
)

// ============================================
// DETALLE DE INGRESOS
// ============================================

data class DetalleIngresosDTO(
    val pagosAlumnos: ConceptoFinancieroDTO,
    val alquileresProfesores: ConceptoFinancieroDTO,
    val total: BigDecimal
)

data class ConceptoFinancieroDTO(
    val concepto: String,
    val cantidad: Int,
    val subtotal: BigDecimal
)

// ============================================
// DETALLE DE EGRESOS
// ============================================

data class DetalleEgresosDTO(
    val comisionesProfesores: ConceptoFinancieroDTO,
    val total: BigDecimal
)

// ============================================
// MOVIMIENTOS INDIVIDUALES
// ============================================

data class MovimientoFinancieroDTO(
    val id: Long,
    val fecha: LocalDate,
    val tipo: TipoMovimiento, // INGRESO, EGRESO
    val categoria: CategoriaMovimiento, // PAGO_ALUMNO, ALQUILER, COMISION
    val concepto: String, // Descripción detallada
    val monto: BigDecimal,
    val alumno: String?, // Si aplica
    val profesor: String?, // Si aplica
    val curso: String? // Si aplica
)

enum class TipoMovimiento {
    INGRESO,
    EGRESO
}

enum class CategoriaMovimiento {
    PAGO_ALUMNO,
    ALQUILER_PROFESOR,
    COMISION_PROFESOR
}

// ============================================
// COMPARACIÓN CON MES ANTERIOR
// ============================================

data class ComparacionMensualDTO(
    val mesAnterior: Int,
    val anioAnterior: Int,
    val ingresosAnterior: BigDecimal,
    val egresosAnterior: BigDecimal,
    val balanceAnterior: BigDecimal,
    val diferenciaIngresos: BigDecimal,
    val diferenciaEgresos: BigDecimal,
    val diferenciaBalance: BigDecimal,
    val porcentajeIngresos: BigDecimal,
    val porcentajeEgresos: BigDecimal,
    val porcentajeBalance: BigDecimal
)

// ============================================
// REQUEST PARA FILTROS
// ============================================

data class ReporteFinancieroRequest(
    val mes: Int, // 1-12
    val anio: Int // 2024, 2025, etc.
)

// ============================================
// DTO PARA EXPORTACIÓN EXCEL
// ============================================

data class ExportarReporteRequest(
    val mes: Int,
    val anio: Int
)

// ============================================
// RESPONSE DE EXPORTACIÓN
// ============================================

data class ExportarReporteResponse(
    val nombreArchivo: String,
    val base64: String, // Archivo Excel en base64
    val mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
)