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

        // Traer TODOS los pagos activos
        val todosPagos = pagoRepository.findAllByFechaBajaIsNull(pageable)

        // Filtrar en memoria según rol
        val pagosFiltrados = todosPagos.content.filter { pago ->
            when (rolActivo) {
                RolType.ADMINISTRADOR, RolType.OFICINA -> {
                    // CURSO (alumnos → instituto en cursos comisión) + ALQUILER (profesores → instituto)
                    when (pago) {
                        is PagoCurso -> pago.inscripcion.curso.tipoCurso == CursoType.COMISION
                        is PagoAlquiler -> true
                        else -> false
                    }
                }

                RolType.PROFESOR -> {
                    val profesorId = usuario.getRolProfesor().id
                    // CURSO (alumnos → profesor en sus cursos alquiler) + COMISION (instituto → profesor)
                    when (pago) {
                        is PagoCurso -> {
                            pago.inscripcion.curso.tipoCurso == CursoType.ALQUILER &&
                                    pago.inscripcion.curso.profesores.any { it.id == profesorId }
                        }

                        is PagoComision -> pago.profesor.id == profesorId
                        else -> false
                    }
                }

                else -> false
            }
        }

        // Filtrar por search (case insensitive)
        val pagosBuscados = if (search.isNullOrBlank()) {
            pagosFiltrados
        } else {
            pagosFiltrados.filter { pago ->
                when (pago) {
                    is PagoCurso -> {
                        pago.inscripcion.curso.nombre.contains(search, ignoreCase = true) ||
                                pago.inscripcion.alumno.usuario.nombre.contains(search, ignoreCase = true) ||
                                pago.inscripcion.alumno.usuario.apellido.contains(search, ignoreCase = true)
                    }

                    is PagoAlquiler -> {
                        pago.curso.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.apellido.contains(search, ignoreCase = true)
                    }

                    is PagoComision -> {
                        pago.curso.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.apellido.contains(search, ignoreCase = true)
                    }

                    else -> false
                }
            }
        }

        // Filtrar por tipo
        val pagosPorTipo = if (tipos.isNullOrEmpty()) {
            pagosBuscados
        } else {
            pagosBuscados.filter { pago -> tipos.contains(pago.tipo) }
        }

        //  Filtrar por meses
        val pagosPorMes = if (meses.isNullOrEmpty()) {
            pagosPorTipo
        } else {
            pagosPorTipo.filter { pago -> meses.contains(pago.fecha.monthValue) }
        }

        //  Ordenar por fecha DESC
        val pagosOrdenados = pagosPorMes.sortedByDescending { it.fecha }

        // Crear página
        val pagosDTO = pagosOrdenados.map { mapPagoToDTO(it) }
        return PageImpl(pagosDTO, pageable, todosPagos.totalElements)
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

        // Traer TODOS los pagos activos
        val todosPagos = pagoRepository.findAllByFechaBajaIsNull(pageable)

        // Filtrar en memoria según rol
        val pagosFiltrados = todosPagos.content.filter { pago ->
            when (rolActivo) {
                RolType.ADMINISTRADOR, RolType.OFICINA -> {
                    // COMISION (instituto → profesores)
                    pago is PagoComision
                }

                RolType.PROFESOR -> {
                    val profesorId = usuario.getRolProfesor().id
                    // ALQUILER (profesor → instituto)
                    pago is PagoAlquiler && pago.profesor.id == profesorId
                }

                RolType.ALUMNO -> {
                    val alumnoId = usuario.getRolAlumno().id
                    // CURSO (alumno → instituto/profesor)
                    pago is PagoCurso && pago.inscripcion.alumno.id == alumnoId
                }

                RolType.PORTERIA -> {
                    false
                }
            }
        }

        // Filtrar por search
        val pagosBuscados = if (search.isNullOrBlank()) {
            pagosFiltrados
        } else {
            pagosFiltrados.filter { pago ->
                when (pago) {
                    is PagoCurso -> pago.inscripcion.curso.nombre.contains(search, ignoreCase = true)
                    is PagoAlquiler -> pago.curso.nombre.contains(search, ignoreCase = true)
                    is PagoComision -> {
                        pago.curso.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.nombre.contains(search, ignoreCase = true) ||
                                pago.profesor.usuario.apellido.contains(search, ignoreCase = true)
                    }

                    else -> false
                }
            }
        }

        // Filtrar por meses
        val pagosPorMes = if (meses.isNullOrEmpty()) {
            pagosBuscados
        } else {
            pagosBuscados.filter { pago -> meses.contains(pago.fecha.monthValue) }
        }

        // Ordenar por fecha DESC
        val pagosOrdenados = pagosPorMes.sortedByDescending { it.fecha }

        // Crear página
        val pagosDTO = pagosOrdenados.map { mapPagoToDTO(it) }
        return PageImpl(pagosDTO, pageable, todosPagos.totalElements)
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

        // Calcular cuotas para liquidación según meses restantes
        val cuotasParaLiquidacion = when (inscripcion.tipoPagoSeleccionado.tipo) {
            PagoType.TOTAL -> {
                // Calcular meses desde HOY hasta fin de curso
                calcularMesesRestantesCurso(inscripcion)
            }

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

    @Transactional(readOnly = true)
    fun calcularPreviewAlquiler(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoAlquilerPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profesor = usuarioRepository.findById(profesorId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") }

        require(curso is CursoAlquiler) {
            "El curso no es de tipo alquiler"
        }

        // Validar que es el profesor del curso
        val profesorRol = profesor.getRolProfesor()
        require(curso.profesores.contains(profesorRol)) {
            "No tienes permiso para pagar alquiler de este curso"
        }

        // Obtener pagos ACTIVOS (sin fechaBaja)
        val pagosActivos = curso.pagosAlquiler.filter { it.fechaBaja == null }

        // SIMPLE: Solo contar cuántos pagos hay
        val cuotasPagadas = pagosActivos.size
        val totalCuotas = curso.cuotasAlquiler

        // Números de cuotas pagadas (1, 2, 3, etc.)
        val numerosCuotasPagadas = (1..cuotasPagadas).toList()

        // Números de cuotas pendientes
        val numerosCuotasPendientes = ((cuotasPagadas + 1)..totalCuotas).toList()

        return PagoAlquilerPreviewDTO(
            cursoId = curso.id,
            cursoNombre = curso.nombre,
            profesores = curso.profesores.map { it.usuario.nombreCompleto() },
            montoPorCuota = curso.precioAlquiler,
            totalCuotas = totalCuotas,
            cuotasPagadas = numerosCuotasPagadas,
            cuotasPendientes = numerosCuotasPendientes,
            puedeRegistrar = numerosCuotasPendientes.isNotEmpty()
        )
    }

    @Transactional(readOnly = true)
    fun calcularPreviewComision(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoComisionPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        require(
            usuario.tieneRol(RolType.ADMINISTRADOR) ||
                    usuario.tieneRol(RolType.OFICINA)
        ) {
            "No tienes permisos para registrar pagos de comisión"
        }

        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") }

        require(curso is CursoComision) {
            "El curso no es de tipo comisión"
        }

        val profesorRol = curso.profesores.find { it.usuario.id == profesorId }
            ?: throw IllegalArgumentException("Profesor no encontrado en el curso")

        // Obtener último pago de comisión del CURSO
        val ultimoPago = pagoRepository.findAll()
            .filterIsInstance<PagoComision>()
            .filter {
                it.curso.id == cursoId &&
                        it.fechaBaja == null
            }
            .maxByOrNull { it.fecha }

        // Calcular fechas del período
        // Si hay un pago anterior, empezar DESPUÉS de ese pago (plusSeconds(1))
        // Si no hay pagos, empezar desde el inicio del curso
        val fechaHoraInicio = ultimoPago?.fecha?.plusSeconds(1)
            ?: curso.fechaInicio.atStartOfDay()

        val ahora = LocalDateTime.now()
        val fechaHoraFinCurso = curso.fechaFin.atTime(23, 59, 59)
        val cursoFinalizado = ahora.isAfter(fechaHoraFinCurso)
        val fechaHoraFin = if (cursoFinalizado) fechaHoraFinCurso else ahora

        // Validar que hay período para liquidar
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

        // Calcular días del período
        val diasPeriodo = ChronoUnit.DAYS.between(
            fechaHoraInicio.toLocalDate(),
            fechaHoraFin.toLocalDate()
        ).toInt() + 1

        // Calcular recaudación del período
        val recaudacion = calcularRecaudacionProrrateada(
            cursoId = cursoId,
            fechaHoraInicio = fechaHoraInicio,
            fechaHoraFin = fechaHoraFin
        )

        // Calcular comisión
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
    fun registrarPagoAlquiler(
        usuarioId: Long,
        cursoId: Long,
        numeroCuota: Int
    ): PagoDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profesorRol = usuario.getRolProfesor()

        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") }

        require(curso is CursoAlquiler) {
            "El curso no es de tipo alquiler"
        }

        require(curso.profesores.contains(profesorRol)) {
            "No tienes permiso para pagar alquiler de este curso"
        }

        //  Validar que no exceda el total de cuotas
        val totalCuotas = curso.cuotasAlquiler
        require(numeroCuota in 1..totalCuotas) {
            "Número de cuota inválido. Debe estar entre 1 y $totalCuotas"
        }

        // Validar que la cuota no esté pagada
        // Contar cuotas pagadas activas
        val cuotasPagadas = curso.pagosAlquiler.count { it.fechaBaja == null }

        // La próxima cuota debe ser cuotasPagadas + 1
        require(numeroCuota == cuotasPagadas + 1) {
            "Debe pagar las cuotas en orden. La próxima cuota a pagar es la ${cuotasPagadas + 1}"
        }

        //  Calcular mes/año según número de cuota
        val fechaInicioCurso = curso.fechaInicio
        val mesYAnioPago = fechaInicioCurso.plusMonths((numeroCuota - 1).toLong())

        //  Crear pago
        val pago = PagoAlquiler(
            monto = curso.precioAlquiler,
            registradoPor = usuario,
            curso = curso,
            profesor = profesorRol,
            mesPago = mesYAnioPago.monthValue,
            anioPago = mesYAnioPago.year
        )

        curso.registrarPagoAlquiler(pago)

        val pagoGuardado = pagoRepository.save(pago)

        return mapPagoToDTO(pagoGuardado)
    }

    @Transactional
    fun registrarPagoComision(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoDTO {
        val preview = calcularPreviewComision(usuarioId, cursoId, profesorId)

        require(preview.puedeRegistrar) {
            preview.mensajeError ?: "No se puede registrar la comisión"
        }

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") } as CursoComision

        val profesorRol = curso.profesores.find { it.usuario.id == profesorId }
            ?: throw IllegalArgumentException("Profesor no encontrado")

        val pago = PagoComision(
            monto = preview.montoComision,
            registradoPor = usuario,
            curso = curso,
            profesor = profesorRol,
            mesPago = preview.fechaFin.monthValue,
            anioPago = preview.fechaFin.year,
            fecha = LocalDateTime.now()
        )

        val pagoGuardado = pagoRepository.save(pago)
        return mapPagoToDTO(pagoGuardado)
    }

    @Transactional
    fun anularPago(
        usuarioId: Long,
        pagoId: Long,
        dto: AnularPagoDTO
    ) {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        require(usuario.tieneRol(RolType.ADMINISTRADOR)) {
            "Solo los administradores pueden anular pagos"
        }

        val pago = pagoRepository.findById(pagoId)
            .orElseThrow { IllegalArgumentException("Pago no encontrado") }

        pago.anular(dto.motivo, usuario)
        pagoRepository.save(pago)
    }

    @Transactional(readOnly = true)
    fun calcularPreviewPago(
        usuarioId: Long,
        inscripcionId: Long,
        aplicarRecargo: Boolean
    ): PagoPreviewDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val inscripcion = inscripcionRepository.findById(inscripcionId)
            .orElseThrow { IllegalArgumentException("Inscripción no encontrada") }

        // Validar permisos
        inscripcion.verificarPermisoEdicion(usuario)

        // Calcular montos
        val montoCuota = inscripcion.calcularMontoPorCuota()

        //Calucular descuento
        val totalDescuento = inscripcion.calcularDescuentoAplicado()

        val recargo = if (aplicarRecargo) {
            inscripcion.calcularRecargoAplicado()
        } else {
            BigDecimal.ZERO
        }

        val montoFinal = if (aplicarRecargo) {
            inscripcion.calcularMontoFinalConRecargo()
        } else {
            montoCuota
        }

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
        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") }

        // Obtener pagos según tipo de curso
        val pagos = when (curso) {
            is CursoAlquiler -> {
                curso.pagosAlquiler
                    .filter { it.fechaBaja == null }
                    .sortedByDescending { it.fecha }
            }

            is CursoComision -> {
                pagoRepository.findAll()
                    .filterIsInstance<PagoComision>()
                    .filter { it.curso.id == cursoId && it.fechaBaja == null }
                    .sortedByDescending { it.fecha }
            }

            else -> emptyList()
        }

        return pagos.map { mapPagoToDTO(it) }
    }

    private fun calcularMesesRestantesCurso(inscripcion: Inscripcion): Int {
        val hoy = LocalDate.now()
        val fechaFinCurso = inscripcion.curso.fechaFin

        // Validar que el curso no haya terminado
        if (hoy.isAfter(fechaFinCurso)) {
            throw IllegalStateException(
                "El curso finalizó el ${fechaFinCurso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}. " +
                        "No se pueden registrar pagos totales en cursos finalizados."
            )
        }

        // Calcular meses entre hoy y fin de curso
        // Usamos withDayOfMonth(1) para comparar por mes completo
        val mesesRestantes = ChronoUnit.MONTHS.between(
            hoy.withDayOfMonth(1),
            fechaFinCurso.withDayOfMonth(1)
        ).toInt() + 1 // +1 para incluir el mes actual

        // Asegurar mínimo 1 mes
        return maxOf(mesesRestantes, 1)
    }

    private fun mapPagoToDTO(pago: Pago): PagoDTO {
        // Verificar que el tipo no sea null
        val tipoPago = try {
            pago.tipo
        } catch (e: Exception) {
            // Si falla, inferir del tipo de clase
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

    private fun calcularRecaudacionProrrateada(
        cursoId: Long,
        fechaHoraInicio: LocalDateTime,
        fechaHoraFin: LocalDateTime
    ): BigDecimal {

        // Obtener TODOS los pagos activos del curso
        val pagosCurso = pagoRepository.findAll()
            .filterIsInstance<PagoCurso>()
            .filter { pago ->
                pago.inscripcion.curso.id == cursoId &&
                        pago.fechaBaja == null
            }

        var recaudacionTotal = BigDecimal.ZERO

        pagosCurso.forEach { pago ->
            val montoPorMes = pago.calcularMontoPorMesParaLiquidacion()
            val cuotas = pago.cuotasParaLiquidacion

            // Calcular usando TIMESTAMP
            val mesesEnPeriodo = calcularMesesEnPeriodo(
                fechaHoraPago = pago.fecha,
                cuotasTotales = cuotas,
                fechaHoraInicio = fechaHoraInicio,
                fechaHoraFin = fechaHoraFin
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

        // Iterar por cada mes del pago
        for (i in 0 until cuotasTotales) {
            val fechaHoraMes = fechaHoraPago.plusMonths(i.toLong())

            // Verificar con TIMESTAMP exacto
            if (!fechaHoraMes.isBefore(fechaHoraInicio) &&
                !fechaHoraMes.isAfter(fechaHoraFin)
            ) {
                mesesEnPeriodo++
            }
        }

        return mesesEnPeriodo
    }
}