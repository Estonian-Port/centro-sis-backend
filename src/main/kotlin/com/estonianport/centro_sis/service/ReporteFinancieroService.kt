package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.CategoriaMovimiento
import com.estonianport.centro_sis.dto.ComparacionMensualDTO
import com.estonianport.centro_sis.dto.ConceptoFinancieroDTO
import com.estonianport.centro_sis.dto.DetalleEgresosDTO
import com.estonianport.centro_sis.dto.DetalleIngresosDTO
import com.estonianport.centro_sis.dto.MovimientoFinancieroDTO
import com.estonianport.centro_sis.dto.ReporteFinancieroMensualDTO
import com.estonianport.centro_sis.dto.ResumenFinancieroDTO
import com.estonianport.centro_sis.dto.TipoMovimiento
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.model.PagoAlquiler
import com.estonianport.centro_sis.model.PagoComision
import com.estonianport.centro_sis.model.PagoCurso
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.repository.PagoAlquilerRepository
import com.estonianport.centro_sis.repository.PagoComisionRepository
import com.estonianport.centro_sis.repository.PagoCursoRepository
import com.estonianport.centro_sis.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class ReporteFinancieroService(
    private val pagoCursoRepository: PagoCursoRepository,
    private val pagoAlquilerRepository: PagoAlquilerRepository,
    private val pagoComisionRepository: PagoComisionRepository,
    private val usuarioRepository: UsuarioRepository
) {

    /**
     * Generar reporte financiero mensual completo
     */
    @Transactional(readOnly = true)
    fun generarReporteMensual(
        mes: Int,
        anio: Int,
        usuarioId: Long
    ): ReporteFinancieroMensualDTO {

        // Validar permisos
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        require(usuario.tieneRol(RolType.ADMINISTRADOR)) {
            "Solo administradores pueden ver reportes financieros"
        }

        // Validar mes y año
        require(mes in 1..12) { "Mes debe estar entre 1 y 12" }
        require(anio in 2020..2030) { "Año inválido" }

        // Obtener rango de fechas del mes
        val yearMonth = YearMonth.of(anio, mes)
        val primerDia = yearMonth.atDay(1)
        val ultimoDia = yearMonth.atEndOfMonth()

        // 1. Calcular ingresos
        val detalleIngresos = calcularIngresos(primerDia, ultimoDia)

        // 2. Calcular egresos
        val detalleEgresos = calcularEgresos(primerDia, ultimoDia)

        // 3. Calcular balance
        val balance = detalleIngresos.total - detalleEgresos.total

        // 4. Obtener comparación con mes anterior
        val comparacion = calcularComparacionMesAnterior(mes, anio, primerDia, ultimoDia)

        // 5. Obtener movimientos detallados
        val movimientos = obtenerMovimientos(primerDia, ultimoDia)

        // 6. Construir resumen
        val resumen = ResumenFinancieroDTO(
            totalIngresos = detalleIngresos.total,
            totalEgresos = detalleEgresos.total,
            balance = balance,
            porcentajeCambioIngresos = comparacion?.porcentajeIngresos,
            porcentajeCambioEgresos = comparacion?.porcentajeEgresos,
            porcentajeCambioBalance = comparacion?.porcentajeBalance
        )

        return ReporteFinancieroMensualDTO(
            mes = mes,
            anio = anio,
            resumen = resumen,
            detalleIngresos = detalleIngresos,
            detalleEgresos = detalleEgresos,
            movimientos = movimientos,
            comparacionMesAnterior = comparacion
        )
    }

    // ============================================
    // CALCULAR INGRESOS (CORREGIDO)
    // ============================================

    private fun calcularIngresos(desde: LocalDate, hasta: LocalDate): DetalleIngresosDTO {

        // ✅ INGRESOS = Pagos de alumnos en cursos COMISION + Alquileres de profesores

        // 1. Pagos de alumnos en cursos COMISION (van al instituto)
        val pagosCurso = pagoCursoRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
            .filter { pagoCurso ->
                // ✅ SOLO contar pagos de cursos tipo COMISION
                pagoCurso.inscripcion.curso is CursoComision
            }

        val totalPagosAlumnos = pagosCurso.sumOf { it.monto }
        val cantidadPagosAlumnos = pagosCurso.size

        // 2. Alquileres pagados por profesores (van al instituto)
        val pagosAlquiler = pagoAlquilerRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
        val totalAlquileres = pagosAlquiler.sumOf { it.monto }
        val cantidadAlquileres = pagosAlquiler.size

        // Total ingresos
        val totalIngresos = totalPagosAlumnos + totalAlquileres

        return DetalleIngresosDTO(
            pagosAlumnos = ConceptoFinancieroDTO(
                concepto = "Pagos de Alumnos (Cursos por Comisión)",
                cantidad = cantidadPagosAlumnos,
                subtotal = totalPagosAlumnos
            ),
            alquileresProfesores = ConceptoFinancieroDTO(
                concepto = "Alquileres de Profesores",
                cantidad = cantidadAlquileres,
                subtotal = totalAlquileres
            ),
            total = totalIngresos
        )
    }

    // ============================================
    // CALCULAR EGRESOS (SIN CAMBIOS)
    // ============================================

    private fun calcularEgresos(desde: LocalDate, hasta: LocalDate): DetalleEgresosDTO {

        // Comisiones pagadas a profesores (salen del instituto)
        val pagosComision = pagoComisionRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
        val totalComisiones = pagosComision.sumOf { it.monto }
        val cantidadComisiones = pagosComision.size

        return DetalleEgresosDTO(
            comisionesProfesores = ConceptoFinancieroDTO(
                concepto = "Comisiones a Profesores",
                cantidad = cantidadComisiones,
                subtotal = totalComisiones
            ),
            total = totalComisiones
        )
    }

    // ============================================
    // OBTENER MOVIMIENTOS DETALLADOS (CORREGIDO)
    // ============================================

    private fun obtenerMovimientos(desde: LocalDate, hasta: LocalDate): List<MovimientoFinancieroDTO> {
        val movimientos = mutableListOf<MovimientoFinancieroDTO>()

        // 1. Pagos de curso (SOLO cursos COMISION)
        val pagosCurso = pagoCursoRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
            .filter { it.inscripcion.curso is CursoComision }

        pagosCurso.forEach { pago ->
            movimientos.add(mapearPagoCurso(pago))
        }

        // 2. Alquileres
        val pagosAlquiler = pagoAlquilerRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
        pagosAlquiler.forEach { pago ->
            movimientos.add(mapearPagoAlquiler(pago))
        }

        // 3. Comisiones
        val pagosComision = pagoComisionRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
        pagosComision.forEach { pago ->
            movimientos.add(mapearPagoComision(pago))
        }

        // Ordenar por fecha descendente
        return movimientos.sortedByDescending { it.fecha }
    }

    private fun mapearPagoCurso(pago: PagoCurso): MovimientoFinancieroDTO {
        val inscripcion = pago.inscripcion
        val alumno = inscripcion.alumno.usuario
        val curso = inscripcion.curso

        return MovimientoFinancieroDTO(
            id = pago.id,
            fecha = pago.fecha,
            tipo = TipoMovimiento.INGRESO,
            categoria = CategoriaMovimiento.PAGO_ALUMNO,
            concepto = "Pago de alumno - ${curso.nombre}",
            monto = pago.monto,
            alumno = "${alumno.nombre} ${alumno.apellido}",
            profesor = null,
            curso = curso.nombre
        )
    }

    private fun mapearPagoAlquiler(pago: PagoAlquiler): MovimientoFinancieroDTO {
        val curso = pago.curso
        val profesor = pago.profesor.usuario

        return MovimientoFinancieroDTO(
            id = pago.id,
            fecha = pago.fecha,
            tipo = TipoMovimiento.INGRESO,
            categoria = CategoriaMovimiento.ALQUILER_PROFESOR,
            concepto = "Alquiler - ${curso.nombre} (${pago.mesPago}/${pago.anioPago})",
            monto = pago.monto,
            alumno = null,
            profesor = "${profesor.nombre} ${profesor.apellido}",
            curso = curso.nombre
        )
    }

    private fun mapearPagoComision(pago: PagoComision): MovimientoFinancieroDTO {
        val curso = pago.curso
        val profesor = pago.profesor.usuario

        return MovimientoFinancieroDTO(
            id = pago.id,
            fecha = pago.fecha,
            tipo = TipoMovimiento.EGRESO,
            categoria = CategoriaMovimiento.COMISION_PROFESOR,
            concepto = "Comisión - ${curso.nombre} (${pago.mesPago}/${pago.anioPago})",
            monto = pago.monto,
            alumno = null,
            profesor = "${profesor.nombre} ${profesor.apellido}",
            curso = curso.nombre
        )
    }

    // ============================================
    // COMPARACIÓN CON MES ANTERIOR (SIN CAMBIOS)
    // ============================================

    private fun calcularComparacionMesAnterior(
        mes: Int,
        anio: Int,
        primerDiaActual: LocalDate,
        ultimoDiaActual: LocalDate
    ): ComparacionMensualDTO? {

        // Calcular mes anterior
        val mesAnterior = if (mes == 1) 12 else mes - 1
        val anioAnterior = if (mes == 1) anio - 1 else anio

        val yearMonthAnterior = YearMonth.of(anioAnterior, mesAnterior)
        val primerDiaAnterior = yearMonthAnterior.atDay(1)
        val ultimoDiaAnterior = yearMonthAnterior.atEndOfMonth()

        // Calcular totales del mes anterior
        val ingresosAnterior = calcularIngresos(primerDiaAnterior, ultimoDiaAnterior).total
        val egresosAnterior = calcularEgresos(primerDiaAnterior, ultimoDiaAnterior).total
        val balanceAnterior = ingresosAnterior - egresosAnterior

        // Calcular totales del mes actual
        val ingresosActual = calcularIngresos(primerDiaActual, ultimoDiaActual).total
        val egresosActual = calcularEgresos(primerDiaActual, ultimoDiaActual).total
        val balanceActual = ingresosActual - egresosActual

        // Calcular diferencias y porcentajes
        val diferenciaIngresos = ingresosActual - ingresosAnterior
        val diferenciaEgresos = egresosActual - egresosAnterior
        val diferenciaBalance = balanceActual - balanceAnterior

        val porcentajeIngresos = calcularPorcentajeCambio(ingresosAnterior, ingresosActual)
        val porcentajeEgresos = calcularPorcentajeCambio(egresosAnterior, egresosActual)
        val porcentajeBalance = calcularPorcentajeCambio(balanceAnterior, balanceActual)

        return ComparacionMensualDTO(
            mesAnterior = mesAnterior,
            anioAnterior = anioAnterior,
            ingresosAnterior = ingresosAnterior,
            egresosAnterior = egresosAnterior,
            balanceAnterior = balanceAnterior,
            diferenciaIngresos = diferenciaIngresos,
            diferenciaEgresos = diferenciaEgresos,
            diferenciaBalance = diferenciaBalance,
            porcentajeIngresos = porcentajeIngresos,
            porcentajeEgresos = porcentajeEgresos,
            porcentajeBalance = porcentajeBalance
        )
    }

    private fun calcularPorcentajeCambio(anterior: BigDecimal, actual: BigDecimal): BigDecimal {
        if (anterior == BigDecimal.ZERO) {
            return if (actual > BigDecimal.ZERO) BigDecimal(100) else BigDecimal.ZERO
        }
        return ((actual - anterior) / anterior * BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }
}