package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.*
import com.estonianport.centro_sis.model.*
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoPagoConcepto
import com.estonianport.centro_sis.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
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
        rolActivo: RolType, // ✅ NUEVO: rol con el que está logueado
        page: Int,
        size: Int,
        search: String?,
        tipos: List<TipoPagoConcepto>?,
        meses: List<Int>?
    ): Page<PagoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val pageable: Pageable = PageRequest.of(page, size)

        // ✅ Traer TODOS los pagos activos
        val todosPagos = pagoRepository.findAllByFechaBajaIsNull(pageable)

        // ✅ Filtrar en memoria según rol
        val pagosFiltrados = todosPagos.content.filter { pago ->
            when (rolActivo) {
                RolType.ADMINISTRADOR, RolType.OFICINA -> {
                    // CURSO (alumnos → instituto en cursos comisión) + ALQUILER (profesores → instituto)
                    when (pago) {
                        is PagoCurso -> pago.inscripcion.curso.tipoCurso == com.estonianport.centro_sis.model.enums.CursoType.COMISION
                        is PagoAlquiler -> true
                        else -> false
                    }
                }

                RolType.PROFESOR -> {
                    val profesorId = usuario.getRolProfesor().id
                    // CURSO (alumnos → profesor en sus cursos alquiler) + COMISION (instituto → profesor)
                    when (pago) {
                        is PagoCurso -> {
                            pago.inscripcion.curso.tipoCurso == com.estonianport.centro_sis.model.enums.CursoType.ALQUILER &&
                                    pago.inscripcion.curso.profesores.any { it.id == profesorId }
                        }

                        is PagoComision -> pago.profesor.id == profesorId
                        else -> false
                    }
                }

                else -> false
            }
        }

        // ✅ Filtrar por search (case insensitive)
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

        // ✅ Filtrar por tipo
        val pagosPorTipo = if (tipos.isNullOrEmpty()) {
            pagosBuscados
        } else {
            pagosBuscados.filter { pago -> tipos.contains(pago.tipo) }
        }

        // ✅ Filtrar por meses
        val pagosPorMes = if (meses.isNullOrEmpty()) {
            pagosPorTipo
        } else {
            pagosPorTipo.filter { pago -> meses.contains(pago.fecha.monthValue) }
        }

        // ✅ Ordenar por fecha DESC
        val pagosOrdenados = pagosPorMes.sortedByDescending { it.fecha }

        // ✅ Crear página
        val pagosDTO = pagosOrdenados.map { mapPagoToDTO(it) }
        return PageImpl(pagosDTO, pageable, todosPagos.totalElements)
    }

    // ========================================
    // PAGOS REALIZADOS
    // ========================================

    @Transactional(readOnly = true)
    fun getPagosRealizados(
        usuarioId: Long,
        rolActivo: RolType, // ✅ NUEVO: rol con el que está logueado
        page: Int,
        size: Int,
        search: String?,
        meses: List<Int>?
    ): Page<PagoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val pageable: Pageable = PageRequest.of(page, size)

        // ✅ Traer TODOS los pagos activos
        val todosPagos = pagoRepository.findAllByFechaBajaIsNull(pageable)

        // ✅ Filtrar en memoria según rol
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

        // ✅ Filtrar por search
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

        // ✅ Filtrar por meses
        val pagosPorMes = if (meses.isNullOrEmpty()) {
            pagosBuscados
        } else {
            pagosBuscados.filter { pago -> meses.contains(pago.fecha.monthValue) }
        }

        // ✅ Ordenar por fecha DESC
        val pagosOrdenados = pagosPorMes.sortedByDescending { it.fecha }

        // ✅ Crear página
        val pagosDTO = pagosOrdenados.map { mapPagoToDTO(it) }
        return PageImpl(pagosDTO, pageable, todosPagos.totalElements)
    }

    // ========================================
    // REGISTRAR PAGOS (sin cambios)
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

        // Validar permisos
        inscripcion.verificarPermisoEdicion(usuario)

        // Registrar pago
        val pago = inscripcion.registrarPago(
            registradoPor = usuario,
            aplicarRecargo = aplicarRecargo
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

        // ✅ Obtener pagos ACTIVOS (sin fechaBaja)
        val pagosActivos = curso.pagosAlquiler.filter { it.fechaBaja == null }

        // ✅ SIMPLE: Solo contar cuántos pagos hay
        val cuotasPagadas = pagosActivos.size
        val totalCuotas = curso.cuotasAlquiler ?: 1

        // ✅ Números de cuotas pagadas (1, 2, 3, etc.)
        val numerosCuotasPagadas = (1..cuotasPagadas).toList()

        // ✅ Números de cuotas pendientes
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

        val profesorRol = curso.profesores.find { it.id == profesorId }
            ?: throw IllegalArgumentException("Profesor no encontrado en el curso")

        // Obtener último pago de comisión
        val ultimoPago = pagoRepository.findAll()
            .filterIsInstance<PagoComision>()
            .filter {
                it.curso.id == cursoId &&
                        it.profesor.id == profesorId &&
                        it.fechaBaja == null
            }
            .maxByOrNull { it.fecha }

        // Calcular fechas del período
        val fechaInicio = ultimoPago?.fecha?.plusDays(1) ?: curso.fechaInicio
        val hoy = LocalDate.now()
        val fechaFinCurso = curso.fechaFin
        val cursoFinalizado = hoy.isAfter(fechaFinCurso)
        val fechaFin = if (cursoFinalizado) fechaFinCurso else hoy

        // Validar que hay período para liquidar
        if (fechaInicio.isAfter(fechaFin)) {
            return PagoComisionPreviewDTO(
                cursoId = curso.id,
                cursoNombre = curso.nombre,
                profesorId = profesorId,
                profesorNombre = profesorRol.usuario.nombreCompleto(),
                porcentajeComision = curso.porcentajeComision,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                diasPeriodo = 0,
                recaudacionPeriodo = BigDecimal.ZERO,
                montoComision = BigDecimal.ZERO,
                puedeRegistrar = false,
                mensajeError = "No hay período pendiente para liquidar"
            )
        }

        // Calcular días del período
        val diasPeriodo = ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1

        // Calcular recaudación del período
        val recaudacion = pagoRepository.findAll()
            .filterIsInstance<PagoCurso>()
            .filter { pago ->
                pago.inscripcion.curso.id == cursoId &&
                        pago.fechaBaja == null &&
                        !pago.fecha.isBefore(fechaInicio) &&
                        !pago.fecha.isAfter(fechaFin)
            }
            .sumOf { it.monto }

        // Calcular comisión
        val montoComision = recaudacion * curso.porcentajeComision

        return PagoComisionPreviewDTO(
            cursoId = curso.id,
            cursoNombre = curso.nombre,
            profesorId = profesorId,
            profesorNombre = profesorRol.usuario.nombreCompleto(),
            porcentajeComision = curso.porcentajeComision,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
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

        // ✅ Validar que no exceda el total de cuotas
        val totalCuotas = curso.cuotasAlquiler ?: 1
        require(numeroCuota in 1..totalCuotas) {
            "Número de cuota inválido. Debe estar entre 1 y $totalCuotas"
        }

        // ✅ SIMPLE: Validar que la cuota no esté pagada
        // Contar cuotas pagadas activas
        val cuotasPagadas = curso.pagosAlquiler.count { it.fechaBaja == null }

        // La próxima cuota debe ser cuotasPagadas + 1
        require(numeroCuota == cuotasPagadas + 1) {
            "Debe pagar las cuotas en orden. La próxima cuota a pagar es la ${cuotasPagadas + 1}"
        }

        // ✅ Calcular mes/año según número de cuota
        val fechaInicioCurso = curso.fechaInicio
        val mesYAnioPago = fechaInicioCurso.plusMonths((numeroCuota - 1).toLong())

        // ✅ Crear pago
        val pago = PagoAlquiler(
            monto = curso.precioAlquiler,
            registradoPor = usuario,
            curso = curso,
            profesor = profesorRol,
            mesPago = mesYAnioPago.monthValue,
            anioPago = mesYAnioPago.year
        )

        // ✅ Agregar al curso (esto actualiza la relación bidireccional)
        curso.registrarPagoAlquiler(pago)

        // ✅ Guardar el pago (Cascade guardará la relación)
        val pagoGuardado = pagoRepository.save(pago)

        // ❌ NO necesitás curso.save() - Cascade se encarga
        // Si guardás el pago, la relación se actualiza automáticamente

        return mapPagoToDTO(pagoGuardado)
    }

    @Transactional
    fun registrarPagoComision(
        usuarioId: Long,
        cursoId: Long,
        profesorId: Long
    ): PagoDTO {
        // Reusar el preview para obtener datos calculados
        val preview = calcularPreviewComision(usuarioId, cursoId, profesorId)

        require(preview.puedeRegistrar) {
            preview.mensajeError ?: "No se puede registrar la comisión"
        }

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val curso = cursoRepository.findById(cursoId)
            .orElseThrow { IllegalArgumentException("Curso no encontrado") } as CursoComision

        val profesorRol = curso.profesores.find { it.id == profesorId }
            ?: throw IllegalArgumentException("Profesor no encontrado")

        val pago = PagoComision(
            monto = preview.montoComision,
            registradoPor = usuario,
            curso = curso,
            profesor = profesorRol,
            mesPago = preview.fechaFin.monthValue,
            anioPago = preview.fechaFin.year
        )

        val pagoGuardado = pagoRepository.save(pago)
        return mapPagoToDTO(pagoGuardado)
    }

    // Helper para calcular número de cuota desde mes/año
    private fun calcularNumeroCuota(pago: PagoAlquiler, curso: CursoAlquiler): Int {
        val fechaPago = pago.fecha
        val fechaInicioCurso = curso.fechaInicio

        val mesesDiferencia = ChronoUnit.MONTHS.between(
            YearMonth.from(fechaInicioCurso),
            YearMonth.from(fechaPago)
        ).toInt()

        return mesesDiferencia + 1
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

    private fun validarPermisoRegistroPagoCurso(
        usuario: Usuario,
        inscripcionId: Long
    ) {
        val roles = usuario.getRolTypes()

        if (roles.contains(RolType.ADMINISTRADOR) || roles.contains(RolType.OFICINA)) {
            return
        }

        if (roles.contains(RolType.PROFESOR)) {
            val inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow { IllegalArgumentException("Inscripción no encontrada") }

            val profesorRol = usuario.getRolProfesor()
            val esSuCurso = inscripcion.curso.profesores.contains(profesorRol)

            require(esSuCurso) {
                "No tienes permiso para registrar pagos de este curso"
            }
            return
        }

        throw IllegalAccessException("No tienes permisos para registrar pagos")
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
        val montoPorCuota = inscripcion.calcularMontoPorCuota()
        val totalDescuento = inscripcion.tipoPagoSeleccionado.monto -
                (montoPorCuota * BigDecimal(inscripcion.tipoPagoSeleccionado.cuotas))

        val recargo = if (aplicarRecargo) {
            montoPorCuota * (inscripcion.curso.recargoAtraso - BigDecimal.ONE)
        } else {
            BigDecimal.ZERO
        }

        val montoFinal = if (aplicarRecargo) {
            montoPorCuota * inscripcion.curso.recargoAtraso
        } else {
            montoPorCuota
        }

        val resumen = inscripcion.obtenerResumenPago()

        return PagoPreviewDTO(
            inscripcionId = inscripcion.id,
            alumnoNombre = inscripcion.alumno.usuario.nombreCompleto(),
            cursoNombre = inscripcion.curso.nombre,
            montoPorCuota = montoPorCuota,
            beneficio = inscripcion.beneficio,
            descuento = totalDescuento,
            recargoPorcentaje = inscripcion.curso.recargoAtraso,
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

// ========================================
// FIX en mapPagoToDTO - Verificar tipo
// ========================================
// Reemplazar en PagoService.kt

    private fun mapPagoToDTO(pago: Pago): PagoDTO {
        // ✅ Verificar que el tipo no sea null
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
                fecha = pago.fecha,
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago, // ✅ Usar variable verificada
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
                fecha = pago.fecha,
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago, // ✅ Usar variable verificada
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
                fecha = pago.fecha,
                fechaBaja = pago.fechaBaja,
                observaciones = pago.observaciones,
                tipo = tipoPago, // ✅ Usar variable verificada
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