package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.*
import com.estonianport.centro_sis.model.*
import com.estonianport.centro_sis.model.enums.CursoType
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoPagoConcepto
import com.estonianport.centro_sis.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class PagoService(
    private val pagoRepository: PagoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val inscripcionRepository: InscripcionRepository,
    private val cursoRepository: CursoRepository
) {

    // ========================================
    // PAGOS RECIBIDOS
    // ========================================

    @Transactional(readOnly = true)
    fun getPagosRecibidos(
        usuarioId: Long,
        rolActivo: RolType,
        page: Int,
        size: Int,
        search: String?,
        tipos: List<TipoPagoConcepto>?,
        meses: List<Int>?
    ): Page<PagoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val pageable: Pageable = PageRequest.of(page, size)

        // 1. Mapeamos los Enums de negocio a las clases polimórficas
        val clasesTipos: List<Class<out Pago>>? = tipos?.map { tipo ->
            when (tipo) {
                TipoPagoConcepto.CURSO -> PagoCurso::class.java
                TipoPagoConcepto.ALQUILER -> PagoAlquiler::class.java
                TipoPagoConcepto.COMISION -> PagoComision::class.java
            }
        }

        // 2. Buscamos solo los IDs paginados con todos los filtros aplicados
        val idsPage: Page<Long> = when (rolActivo) {
            RolType.ADMINISTRADOR, RolType.OFICINA -> {
                pagoRepository.findRecibidosIdsForAdmin(search, clasesTipos, meses, pageable)
            }
            RolType.PROFESOR -> {
                val profesorId = usuario.getRolProfesor().id
                pagoRepository.findRecibidosIdsForProfesor(profesorId, search, clasesTipos, meses, pageable)
            }
            else -> Page.empty(pageable)
        }

        // Si no hay resultados, retornamos vacío
        if (idsPage.isEmpty) {
            return Page.empty(pageable)
        }

        // 3. Hidratamos TODAS las relaciones necesarias en UNA sola query
        val pagosConGrafo = pagoRepository.findWithGraphByIds(idsPage.content)
            .associateBy { it.id }

        // 4. Mapeamos a DTO respetando el orden original
        val pagosDTO = idsPage.content.mapNotNull { id ->
            pagosConGrafo[id]?.let { mapPagoToDTO(it) }
        }

        return PageImpl(pagosDTO, pageable, idsPage.totalElements)
    }

    // ========================================
    // PAGOS REALIZADOS
    // ========================================

    @Transactional(readOnly = true)
    fun getPagosRealizados(
        usuarioId: Long,
        rolActivo: RolType,
        page: Int,
        size: Int,
        search: String?,
        meses: List<Int>?
    ): Page<PagoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val pageable: Pageable = PageRequest.of(page, size)

        // 1. Buscamos solo los IDs paginados desde la BD
        val idsPage: Page<Long> = when (rolActivo) {
            RolType.ADMINISTRADOR, RolType.OFICINA -> {
                pagoRepository.findRealizadosIdsForAdmin(search, meses, pageable)
            }
            RolType.PROFESOR -> {
                val profesorId = usuario.getRolProfesor().id
                pagoRepository.findRealizadosIdsForProfesor(profesorId, search, meses, pageable)
            }
            RolType.ALUMNO -> {
                val alumnoId = usuario.getRolAlumno().id
                pagoRepository.findRealizadosIdsForAlumno(alumnoId, search, meses, pageable)
            }
            else -> Page.empty(pageable)
        }

        if (idsPage.isEmpty) {
            return Page.empty(pageable)
        }

        // 2. Hidratamos todos los objetos en una sola query
        val pagosConGrafo = pagoRepository.findWithGraphByIds(idsPage.content)
            .associateBy { it.id }

        // 3. Mapeamos respetando el orden
        val pagosDTO = idsPage.content.mapNotNull { id ->
            pagosConGrafo[id]?.let { mapPagoToDTO(it) }
        }

        return PageImpl(pagosDTO, pageable, idsPage.totalElements)
    }

    // ========================================
    // REGISTRAR PAGOS
    // ========================================

    @Transactional
    fun registrarPagoCurso(
        usuarioId: Long,
        inscripcionId: Long,
        aplicarRecargo: Boolean
    ): PagoDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val inscripcion = inscripcionRepository.findById(inscripcionId)
            .orElseThrow { IllegalArgumentException("Inscripción no encontrada") }

        inscripcion.verificarPermisoEdicion(usuario)

        val cuotasParaLiquidacion = when (inscripcion.tipoPagoSeleccionado.tipo) {
            PagoType.TOTAL -> calcularMesesRestantesCurso(inscripcion)
            PagoType.MENSUAL -> 1
        }

        val pago = inscripcion.registrarPago(
            registradoPor = usuario,
            aplicarRecargo = aplicarRecargo,
            cuotasParaLiquidacion = cuotasParaLiquidacion
        )

        val pagoGuardado = pagoRepository.save(pago)
        inscripcionRepository.save(inscripcion)

        return mapPagoToDTO(pagoGuardado)
    }

    @Transactional
    fun registrarPagoAlquiler(
        usuarioId: Long,
        cursoId: Long,
        numeroCuota: Int
    ): PagoDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        require(usuario.tieneRol(RolType.ADMINISTRADOR) || usuario.tieneRol(RolType.OFICINA)) {
            "Solo administradores pueden registrar pagos de alquiler"
        }

        val curso = cursoRepository.findById(cursoId).orElseThrow {
            IllegalArgumentException("Curso no encontrado")
        }
        require(curso is CursoAlquiler) { "El curso debe ser de tipo alquiler" }

        // Crear el PagoAlquiler con los parámetros correctos
        val pagoAlquiler = PagoAlquiler(
            monto = curso.precioAlquiler,
            registradoPor = usuario,
            curso = curso,
            profesor = curso.profesores.firstOrNull() ?: throw IllegalArgumentException("El curso debe tener al menos un profesor"),
            mesPago = numeroCuota
        )

        val pago = curso.registrarPagoAlquiler(pagoAlquiler)
        val pagoGuardado = pagoRepository.save(pago)
        cursoRepository.save(curso)

        return mapPagoToDTO(pagoGuardado)
    }

    @Transactional
    fun registrarPagoComision(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        require(usuario.tieneRol(RolType.ADMINISTRADOR) || usuario.tieneRol(RolType.OFICINA)) {
            "Solo administradores pueden registrar pagos de comisión"
        }

        val curso = cursoRepository.findById(cursoId).orElseThrow {
            IllegalArgumentException("Curso no encontrado")
        }
        require(curso is CursoComision) { "El curso debe ser de tipo comisión" }

        val profesor = curso.profesores.find { it.id == profesorId }
            ?: throw IllegalArgumentException("Profesor no encontrado en el curso")

        val pago = curso.registrarPagoComision(
            profesor = profesor,
            recibioPago = usuario
        )

        val pagoGuardado = pagoRepository.save(pago)
        return mapPagoToDTO(pagoGuardado)
    }

    // ========================================
    // PREVIEW DE PAGOS
    // ========================================

    @Transactional(readOnly = true)
    fun calcularPreviewAlquiler(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoAlquilerPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado")
        }
        require(usuario.tieneRol(RolType.ADMINISTRADOR) || usuario.tieneRol(RolType.OFICINA)) {
            "Solo administradores pueden ver previews de alquiler"
        }

        val curso = cursoRepository.findById(cursoId).orElseThrow {
            IllegalArgumentException("Curso no encontrado")
        }
        require(curso is CursoAlquiler) { "El curso debe ser de tipo alquiler" }

        // Obtener nombres de profesores del curso
        val profesoresNombres = curso.profesores.map { it.usuario.nombreCompleto() }

        // Obtener las cuotas pagadas (basadas en mesPago de los pagosAlquiler)
        val pagosPorCuota = curso.pagosAlquiler
            .filter { it.fechaBaja == null }
            .mapNotNull { it.mesPago }
            .toSet()

        // Calcular cuotas totales basadas en duración del curso
        val fechaInicio = curso.fechaInicio
        val fechaFin = curso.fechaFin
        val mesesDiferencia = (fechaFin.year * 12 + fechaFin.monthValue) -
                (fechaInicio.year * 12 + fechaInicio.monthValue)
        val cuotasTotales = maxOf(mesesDiferencia + 1, 1)

        val cuotasPagadas = pagosPorCuota.sorted()
        val cuotasPendientes = (1..cuotasTotales).filter { !pagosPorCuota.contains(it) }

        return PagoAlquilerPreviewDTO(
            cursoId = curso.id,
            cursoNombre = curso.nombre,
            profesores = profesoresNombres,
            montoPorCuota = curso.precioAlquiler,
            totalCuotas = cuotasTotales,
            cuotasPagadas = cuotasPagadas,
            cuotasPendientes = cuotasPendientes,
            puedeRegistrar = cuotasPendientes.isNotEmpty()
        )
    }

    @Transactional(readOnly = true)
    fun calcularPreviewComision(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoComisionPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado")
        }
        require(usuario.tieneRol(RolType.ADMINISTRADOR) || usuario.tieneRol(RolType.OFICINA)) {
            "Solo administradores pueden ver previews de comisión"
        }

        val curso = cursoRepository.findById(cursoId).orElseThrow {
            IllegalArgumentException("Curso no encontrado")
        }
        require(curso is CursoComision) { "El curso debe ser de tipo comisión" }

        val profesorRol = curso.profesores.find { it.id == profesorId } ?: throw IllegalArgumentException(
            "Profesor no encontrado en el curso"
        )

        val ultimoPago = pagoRepository.findUltimoPagoComisionActivo(cursoId, profesorRol.usuario.id)
        val fechaHoraInicio = ultimoPago?.fecha?.plusSeconds(1) ?: curso.fechaInicio.atStartOfDay()
        val ahora = LocalDateTime.now()
        val fechaHoraFinCurso = curso.fechaFin.atTime(23, 59, 59)
        val fechaHoraFin = if (ahora.isAfter(fechaHoraFinCurso)) fechaHoraFinCurso else ahora

        if (fechaHoraInicio.isAfter(fechaHoraFin)) {
            return PagoComisionPreviewDTO(
                cursoId = curso.id,
                cursoNombre = curso.nombre,
                profesorId = profesorId,
                profesorNombre = profesorRol.usuario.nombreCompleto(),
                porcentajeComision = curso.porcentajeComision,
                fechaInicio = fechaHoraInicio.toLocalDate(),
                fechaFin = fechaHoraFin.toLocalDate(),
                diasPeriodo = 0,
                recaudacionPeriodo = BigDecimal.ZERO,
                montoComision = BigDecimal.ZERO,
                puedeRegistrar = false,
                mensajeError = "No hay período pendiente para liquidar"
            )
        }

        val diasPeriodo = ChronoUnit.DAYS.between(fechaHoraInicio.toLocalDate(), fechaHoraFin.toLocalDate()).toInt() + 1
        val recaudacion = calcularRecaudacionProrrateada(cursoId, fechaHoraInicio, fechaHoraFin)
        val montoComision = recaudacion * curso.porcentajeComision

        return PagoComisionPreviewDTO(
            cursoId = curso.id,
            cursoNombre = curso.nombre,
            profesorId = profesorId,
            profesorNombre = profesorRol.usuario.nombreCompleto(),
            porcentajeComision = curso.porcentajeComision,
            fechaInicio = fechaHoraInicio.toLocalDate(),
            fechaFin = fechaHoraFin.toLocalDate(),
            diasPeriodo = diasPeriodo,
            recaudacionPeriodo = recaudacion,
            montoComision = montoComision,
            puedeRegistrar = true
        )
    }

    @Transactional
    fun anularPago(usuarioId: Long, pagoId: Long, dto: AnularPagoDTO) {
        val usuario = usuarioRepository.findById(usuarioId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado")
        }
        require(usuario.tieneRol(RolType.ADMINISTRADOR)) { "Solo los administradores pueden anular pagos" }

        val pago = pagoRepository.findById(pagoId).orElseThrow {
            IllegalArgumentException("Pago no encontrado")
        }
        pago.anular(dto.motivo, usuario)
        pagoRepository.save(pago)
    }

    @Transactional(readOnly = true)
    fun calcularPreviewPago(usuarioId: Long, inscripcionId: Long, aplicarRecargo: Boolean): PagoPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado")
        }
        val inscripcion = inscripcionRepository.findById(inscripcionId).orElseThrow {
            IllegalArgumentException("Inscripción no encontrada")
        }

        inscripcion.verificarPermisoEdicion(usuario)

        val montoCuota = inscripcion.calcularMontoPorCuota()
        val totalDescuento = inscripcion.calcularDescuentoAplicado()
        val recargo = if (aplicarRecargo) inscripcion.calcularRecargoAplicado() else BigDecimal.ZERO
        val montoFinal = if (aplicarRecargo) inscripcion.calcularMontoFinalConRecargo() else montoCuota
        val resumen = inscripcion.obtenerResumenPago()

        return PagoPreviewDTO(
            inscripcionId = inscripcion.id,
            alumnoNombre = inscripcion.alumno.usuario.nombreCompleto(),
            cursoNombre = inscripcion.curso.nombre,
            montoPorCuota = inscripcion.tipoPagoSeleccionado.monto,
            beneficio = inscripcion.beneficio,
            descuento = totalDescuento,
            recargoPorcentaje = (inscripcion.curso.recargoAtraso - BigDecimal.ONE) * BigDecimal(100),
            recargo = recargo,
            montoFinal = montoFinal,
            aplicaRecargo = aplicarRecargo,
            cuotasPagadas = resumen.cuotasPagadas,
            cuotasTotales = resumen.cuotasTotales,
            cuotasEsperadas = resumen.cuotasEsperadas,
            cuotasAtrasadas = resumen.cuotasAtrasadas,
            puedeRegistrar = inscripcion.puedeRegistrarPago()
        )
    }

    @Transactional(readOnly = true)
    fun getPagosPorCurso(cursoId: Long): List<PagoDTO> {
        val curso = cursoRepository.findById(cursoId).orElseThrow {
            IllegalArgumentException("Curso no encontrado")
        }

        val pagos = when (curso) {
            is CursoAlquiler -> {
                curso.pagosAlquiler.filter { it.fechaBaja == null }.sortedByDescending { it.fecha }
            }
            is CursoComision -> {
                pagoRepository.findComisionesByCursoId(cursoId).sortedByDescending { it.fecha }
            }
            else -> emptyList()
        }

        return pagos.map { mapPagoToDTO(it) }
    }

    // ========================================
    // MÉTODOS PRIVADOS AUXILIARES
    // ========================================

    private fun calcularMesesRestantesCurso(inscripcion: Inscripcion): Int {
        val hoy = LocalDate.now()
        val fechaFinCurso = inscripcion.curso.fechaFin

        if (hoy.year > fechaFinCurso.year || (hoy.year == fechaFinCurso.year && hoy.monthValue > fechaFinCurso.monthValue)) {
            throw IllegalStateException("El curso finalizó. No se pueden registrar pagos totales.")
        }

        val mesesDiferencia = (fechaFinCurso.year * 12 + fechaFinCurso.monthValue) - (hoy.year * 12 + hoy.monthValue)
        return maxOf(mesesDiferencia + 1, 1)
    }

    private fun calcularRecaudacionProrrateada(
        cursoId: Long,
        fechaHoraInicio: LocalDateTime,
        fechaHoraFin: LocalDateTime
    ): BigDecimal {
        // Obtener todos los pagos del repositorio
        val todosLosPagos = pagoRepository.findAll()

        // Filtrar en memoria para PagoCurso de este curso
        val pagosCurso = todosLosPagos
            .filterIsInstance<PagoCurso>()
            .filter { pago -> pago.inscripcion.curso.id == cursoId && pago.fechaBaja == null }

        var recaudacionTotal = BigDecimal.ZERO

        pagosCurso.forEach { pago ->
            val montoPorMes = pago.calcularMontoPorMesParaLiquidacion()

            val mesesEnPeriodo = calcularMesesEnPeriodo(
                pago.fecha,
                pago.cuotasParaLiquidacion,
                fechaHoraInicio,
                fechaHoraFin
            )

            recaudacionTotal += montoPorMes * BigDecimal(mesesEnPeriodo)
        }

        return recaudacionTotal
    }

    private fun calcularMesesEnPeriodo(
        fechaHoraPago: LocalDateTime,
        cuotasTotales: Int,
        fechaHoraInicio: LocalDateTime,
        fechaHoraFin: LocalDateTime
    ): Int {
        var mesesEnPeriodo = 0
        for (i in 0 until cuotasTotales) {
            val fechaHoraMes = fechaHoraPago.plusMonths(i.toLong())
            if (!fechaHoraMes.isBefore(fechaHoraInicio) && !fechaHoraMes.isAfter(fechaHoraFin)) {
                mesesEnPeriodo++
            }
        }
        return mesesEnPeriodo
    }

    private fun mapPagoToDTO(pago: Pago): PagoDTO {
        val tipoPago = try {
            pago.tipo
        } catch (e: Exception) {
            when (pago) {
                is PagoCurso -> TipoPagoConcepto.CURSO
                is PagoAlquiler -> TipoPagoConcepto.ALQUILER
                is PagoComision -> TipoPagoConcepto.COMISION
                else -> throw IllegalArgumentException("Tipo de pago desconocido")
            }
        }

        return when (pago) {
            is PagoCurso -> PagoDTO(
                id = pago.id,
                monto = pago.monto,
                fecha = pago.fecha.toLocalDate(),
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago,
                cursoId = pago.inscripcion.curso.id,
                cursoNombre = pago.inscripcion.curso.nombre,
                usuarioPagaId = pago.inscripcion.alumno.usuario.id,
                usuarioPagaNombre = pago.inscripcion.alumno.usuario.nombre,
                usuarioPagaApellido = pago.inscripcion.alumno.usuario.apellido,
                usuarioRecibeId = null,
                usuarioRecibeNombre = null,
                usuarioRecibeApellido = null,
                inscripcionId = pago.inscripcion.id,
                retraso = pago.conRecargo,
                beneficioAplicado = pago.beneficioAplicado
            )
            is PagoAlquiler -> PagoDTO(
                id = pago.id,
                monto = pago.monto,
                fecha = pago.fecha.toLocalDate(),
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago,
                cursoId = pago.curso.id,
                cursoNombre = pago.curso.nombre,
                usuarioPagaId = pago.profesor.usuario.id,
                usuarioPagaNombre = pago.profesor.usuario.nombre,
                usuarioPagaApellido = pago.profesor.usuario.apellido,
                usuarioRecibeId = null,
                usuarioRecibeNombre = "Instituto",
                usuarioRecibeApellido = null
            )
            is PagoComision -> PagoDTO(
                id = pago.id,
                monto = pago.monto,
                fecha = pago.fecha.toLocalDate(),
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago,
                cursoId = pago.curso.id,
                cursoNombre = pago.curso.nombre,
                usuarioPagaId = null,
                usuarioPagaNombre = "Instituto",
                usuarioPagaApellido = null,
                usuarioRecibeId = pago.profesor.usuario.id,
                usuarioRecibeNombre = pago.profesor.usuario.nombre,
                usuarioRecibeApellido = pago.profesor.usuario.apellido
            )
            else -> throw IllegalArgumentException("Tipo de pago desconocido")
        }
    }
}