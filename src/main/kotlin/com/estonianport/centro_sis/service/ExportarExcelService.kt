package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.ExportarReporteResponse
import com.estonianport.centro_sis.dto.ReporteFinancieroMensualDTO
import com.estonianport.centro_sis.dto.TipoMovimiento
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Base64

@Service
class ExportarExcelService(
    private val reporteFinancieroService: ReporteFinancieroService
) {

    fun exportarReporteFinanciero(
        mes: Int,
        anio: Int,
        usuarioId: Long
    ): ExportarReporteResponse {

        // 1. Obtener datos del reporte
        val reporte = reporteFinancieroService.generarReporteMensual(mes, anio, usuarioId)

        // 2. Crear workbook
        val workbook = XSSFWorkbook()

        // 3. Crear hojas
        crearHojaResumen(workbook, reporte)
        crearHojaMovimientos(workbook, reporte)

        // 4. Convertir a bytes
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        // 5. Codificar en Base64
        val base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray())

        // 6. Generar nombre de archivo
        val nombreArchivo = "Reporte_Financiero_${mes}_${anio}.xlsx"

        return ExportarReporteResponse(
            nombreArchivo = nombreArchivo,
            base64 = base64
        )
    }

    // ============================================
    // HOJA 1: RESUMEN
    // ============================================

    private fun crearHojaResumen(workbook: Workbook, reporte: ReporteFinancieroMensualDTO) {
        val sheet = workbook.createSheet("Resumen")

        // Estilos
        val headerStyle = crearEstiloHeader(workbook)
        val titleStyle = crearEstiloTitulo(workbook)
        val moneyStyle = crearEstiloMoneda(workbook)
        val percentStyle = crearEstiloPorcentaje(workbook)

        var rowNum = 0

        // TÍTULO
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("REPORTE FINANCIERO - ${getNombreMes(reporte.mes)} ${reporte.anio}")
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3))

        rowNum++ // Espacio

        // RESUMEN GENERAL
        val resumenRow = sheet.createRow(rowNum++)
        resumenRow.createCell(0).setCellValue("RESUMEN GENERAL")
        resumenRow.getCell(0).cellStyle = headerStyle

        // Total Ingresos
        val ingresosRow = sheet.createRow(rowNum++)
        ingresosRow.createCell(0).setCellValue("Total Ingresos")
        val ingresosCell = ingresosRow.createCell(1)
        ingresosCell.setCellValue(reporte.resumen.totalIngresos.toDouble())
        ingresosCell.cellStyle = moneyStyle

        // Cambio vs mes anterior
        reporte.resumen.porcentajeCambioIngresos?.let { cambio ->
            val cambioCell = ingresosRow.createCell(2)
            cambioCell.setCellValue(cambio.toDouble())
            cambioCell.cellStyle = percentStyle
        }

        // Total Egresos
        val egresosRow = sheet.createRow(rowNum++)
        egresosRow.createCell(0).setCellValue("Total Egresos")
        val egresosCell = egresosRow.createCell(1)
        egresosCell.setCellValue(reporte.resumen.totalEgresos.toDouble())
        egresosCell.cellStyle = moneyStyle

        reporte.resumen.porcentajeCambioEgresos?.let { cambio ->
            val cambioCell = egresosRow.createCell(2)
            cambioCell.setCellValue(cambio.toDouble())
            cambioCell.cellStyle = percentStyle
        }

        // Balance
        val balanceRow = sheet.createRow(rowNum++)
        balanceRow.createCell(0).setCellValue("Balance")
        val balanceCell = balanceRow.createCell(1)
        balanceCell.setCellValue(reporte.resumen.balance.toDouble())
        balanceCell.cellStyle = moneyStyle

        reporte.resumen.porcentajeCambioBalance?.let { cambio ->
            val cambioCell = balanceRow.createCell(2)
            cambioCell.setCellValue(cambio.toDouble())
            cambioCell.cellStyle = percentStyle
        }

        rowNum++ // Espacio

        // DETALLE DE INGRESOS
        val ingresosHeaderRow = sheet.createRow(rowNum++)
        ingresosHeaderRow.createCell(0).setCellValue("DETALLE DE INGRESOS")
        ingresosHeaderRow.getCell(0).cellStyle = headerStyle

        val ingresosHeaderRow2 = sheet.createRow(rowNum++)
        ingresosHeaderRow2.createCell(0).setCellValue("Concepto")
        ingresosHeaderRow2.createCell(1).setCellValue("Cantidad")
        ingresosHeaderRow2.createCell(2).setCellValue("Subtotal")
        ingresosHeaderRow2.cellIterator().forEach { it.cellStyle = headerStyle }

        // Pagos de alumnos
        val pagosAlumnosRow = sheet.createRow(rowNum++)
        pagosAlumnosRow.createCell(0).setCellValue(reporte.detalleIngresos.pagosAlumnos.concepto)
        pagosAlumnosRow.createCell(1).setCellValue(reporte.detalleIngresos.pagosAlumnos.cantidad.toDouble())
        val pagosAlumnosCell = pagosAlumnosRow.createCell(2)
        pagosAlumnosCell.setCellValue(reporte.detalleIngresos.pagosAlumnos.subtotal.toDouble())
        pagosAlumnosCell.cellStyle = moneyStyle

        // Alquileres
        val alquileresRow = sheet.createRow(rowNum++)
        alquileresRow.createCell(0).setCellValue(reporte.detalleIngresos.alquileresProfesores.concepto)
        alquileresRow.createCell(1).setCellValue(reporte.detalleIngresos.alquileresProfesores.cantidad.toDouble())
        val alquileresCell = alquileresRow.createCell(2)
        alquileresCell.setCellValue(reporte.detalleIngresos.alquileresProfesores.subtotal.toDouble())
        alquileresCell.cellStyle = moneyStyle

        // Total Ingresos
        val totalIngresosRow = sheet.createRow(rowNum++)
        totalIngresosRow.createCell(0).setCellValue("TOTAL INGRESOS")
        totalIngresosRow.getCell(0).cellStyle = headerStyle
        val totalIngresosCell = totalIngresosRow.createCell(2)
        totalIngresosCell.setCellValue(reporte.detalleIngresos.total.toDouble())
        totalIngresosCell.cellStyle = moneyStyle

        rowNum++ // Espacio

        // DETALLE DE EGRESOS
        val egresosHeaderRow = sheet.createRow(rowNum++)
        egresosHeaderRow.createCell(0).setCellValue("DETALLE DE EGRESOS")
        egresosHeaderRow.getCell(0).cellStyle = headerStyle

        val egresosHeaderRow2 = sheet.createRow(rowNum++)
        egresosHeaderRow2.createCell(0).setCellValue("Concepto")
        egresosHeaderRow2.createCell(1).setCellValue("Cantidad")
        egresosHeaderRow2.createCell(2).setCellValue("Subtotal")
        egresosHeaderRow2.cellIterator().forEach { it.cellStyle = headerStyle }

        // Comisiones
        val comisionesRow = sheet.createRow(rowNum++)
        comisionesRow.createCell(0).setCellValue(reporte.detalleEgresos.comisionesProfesores.concepto)
        comisionesRow.createCell(1).setCellValue(reporte.detalleEgresos.comisionesProfesores.cantidad.toDouble())
        val comisionesCell = comisionesRow.createCell(2)
        comisionesCell.setCellValue(reporte.detalleEgresos.comisionesProfesores.subtotal.toDouble())
        comisionesCell.cellStyle = moneyStyle

        // Total Egresos
        val totalEgresosRow = sheet.createRow(rowNum++)
        totalEgresosRow.createCell(0).setCellValue("TOTAL EGRESOS")
        totalEgresosRow.getCell(0).cellStyle = headerStyle
        val totalEgresosCell = totalEgresosRow.createCell(2)
        totalEgresosCell.setCellValue(reporte.detalleEgresos.total.toDouble())
        totalEgresosCell.cellStyle = moneyStyle

        // Ajustar ancho de columnas
        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }
    }

    // ============================================
    // HOJA 2: MOVIMIENTOS DETALLADOS
    // ============================================

    private fun crearHojaMovimientos(workbook: Workbook, reporte: ReporteFinancieroMensualDTO) {
        val sheet = workbook.createSheet("Movimientos Detallados")

        val headerStyle = crearEstiloHeader(workbook)
        val moneyStyle = crearEstiloMoneda(workbook)
        val dateStyle = crearEstiloFecha(workbook)

        var rowNum = 0

        // Headers
        val headerRow = sheet.createRow(rowNum++)
        headerRow.createCell(0).setCellValue("Fecha")
        headerRow.createCell(1).setCellValue("Tipo")
        headerRow.createCell(2).setCellValue("Categoría")
        headerRow.createCell(3).setCellValue("Concepto")
        headerRow.createCell(4).setCellValue("Alumno")
        headerRow.createCell(5).setCellValue("Profesor")
        headerRow.createCell(6).setCellValue("Curso")
        headerRow.createCell(7).setCellValue("Monto")
        headerRow.cellIterator().forEach { it.cellStyle = headerStyle }

        // Datos
        reporte.movimientos.forEach { movimiento ->
            val row = sheet.createRow(rowNum++)

            val fechaCell = row.createCell(0)
            fechaCell.setCellValue(movimiento.fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            fechaCell.cellStyle = dateStyle

            row.createCell(1).setCellValue(movimiento.tipo.name)
            row.createCell(2).setCellValue(movimiento.categoria.name)
            row.createCell(3).setCellValue(movimiento.concepto)
            row.createCell(4).setCellValue(movimiento.alumno ?: "-")
            row.createCell(5).setCellValue(movimiento.profesor ?: "-")
            row.createCell(6).setCellValue(movimiento.curso ?: "-")

            val montoCell = row.createCell(7)
            val montoValue = if (movimiento.tipo == TipoMovimiento.EGRESO) {
                -movimiento.monto.toDouble()
            } else {
                movimiento.monto.toDouble()
            }
            montoCell.setCellValue(montoValue)
            montoCell.cellStyle = moneyStyle
        }

        // Ajustar ancho de columnas
        for (i in 0..7) {
            sheet.autoSizeColumn(i)
        }
    }

    // ============================================
    // ESTILOS
    // ============================================

    private fun crearEstiloHeader(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 12
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        return style
    }

    private fun crearEstiloTitulo(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 16
        style.setFont(font)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun crearEstiloMoneda(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("$#,##0.00")
        return style
    }

    private fun crearEstiloPorcentaje(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("0.00%")
        return style
    }

    private fun crearEstiloFecha(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("dd/mm/yyyy")
        return style
    }

    private fun getNombreMes(mes: Int): String {
        return when (mes) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> "Mes desconocido"
        }
    }
}