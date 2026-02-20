package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.ExportarReporteResponse
import com.estonianport.centro_sis.dto.ReporteFinancieroMensualDTO
import com.estonianport.centro_sis.service.ExportarExcelService
import com.estonianport.centro_sis.service.ReporteFinancieroService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reportes/financiero")
@CrossOrigin(origins = ["*"])
class ReporteFinancieroController(
    private val reporteFinancieroService: ReporteFinancieroService,
    private val exportarExcelService: ExportarExcelService
) {

    /**
     * Obtener reporte financiero mensual
     */
    @GetMapping("/{usuarioId}")
    fun obtenerReporteMensual(
        @PathVariable usuarioId: Long,
        @RequestParam mes: Int,
        @RequestParam anio: Int
    ): ResponseEntity<ReporteFinancieroMensualDTO> {
        val reporte = reporteFinancieroService.generarReporteMensual(mes, anio, usuarioId)
        return ResponseEntity.ok(reporte)
    }

    /**
     * Exportar reporte financiero a Excel
     */
    @GetMapping("/{usuarioId}/exportar")
    fun exportarReporte(
        @PathVariable usuarioId: Long,
        @RequestParam mes: Int,
        @RequestParam anio: Int
    ): ResponseEntity<ExportarReporteResponse> {
        val excel = exportarExcelService.exportarReporteFinanciero(mes, anio, usuarioId)
        return ResponseEntity.ok(excel)
    }
}