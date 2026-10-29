package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.common.errors.ConflictException
import com.estonianport.centro_sis.dto.AccesoDTO
import com.estonianport.centro_sis.dto.AlertaPagosDTO
import com.estonianport.centro_sis.dto.CursoAtrasoDTO
import com.estonianport.centro_sis.dto.EstadisticasAccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoDTO
import com.estonianport.centro_sis.model.Acceso
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.estonianport.centro_sis.repository.AccesoRepository
import com.estonianport.centro_sis.repository.InscripcionRepository
import com.estonianport.centro_sis.repository.UsuarioRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class AccesoService(
    private val accesoRepository: AccesoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val inscripcionRepository: InscripcionRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // =============================================
    // Registrar acceso por QR
    // =============================================

    @Transactional
    fun registrarAccesoQR(usuarioId: Long): AccesoDTO {
        val usuario = usuarioRepository.findAccesoViewById(usuarioId)
            ?: throw NoSuchElementException("Usuario no encontrado")

        val nombre = "${usuario.nombre.orEmpty()} ${usuario.apellido.orEmpty()}".trim()
        if (usuario.estado == EstadoType.BAJA) {
            throw ConflictException("El usuario $nombre fue dado de baja")
        }

        val ahora = AppTime.ahora()                       // una sola lectura del reloj
        val inicioHoy = ahora.toLocalDate().atStartOfDay()

        if (accesoRepository.existeAccesoEnRango(usuarioId, inicioHoy, inicioHoy.plusDays(1))) {
            throw ConflictException("El usuario $nombre ya registró un acceso hoy")
        }

        val acceso = try {
            accesoRepository.saveAndFlush(
                Acceso(
                    usuario = usuarioRepository.getReferenceById(usuarioId),
                    fechaHora = ahora,
                    tipoAcceso = TipoAcceso.QR
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException("El usuario $nombre ya registró un acceso hoy")
        }

        val alertaPagos = try {
            armarAlertaPagos(usuarioId)
        } catch (e: Exception) {
            log.warn("No se pudo calcular alerta de pagos para usuario $usuarioId", e)
            null
        }

        return AccesoDTO(
            id = acceso.id,
            usuarioId = usuario.id,
            usuarioNombre = usuario.nombre,
            usuarioApellido = usuario.apellido,
            usuarioDni = usuario.dni,
            fechaHora = acceso.fechaHora,
            tipoAcceso = TipoAcceso.QR,
            alertaPagos = alertaPagos
        )
    }

    // =============================================
    // Verificar pagos atrasados
    // =============================================

    private fun armarAlertaPagos(usuarioId: Long): AlertaPagosDTO? {
        val atrasadas = inscripcionRepository.findAtrasadasByUsuarioId(usuarioId)
        if (atrasadas.isEmpty()) return null

        val cursos = atrasadas.map {
            CursoAtrasoDTO(it.curso.nombre, it.calcularCuotasAtrasadas(), it.calcularDeudaPendiente())
        }
        return AlertaPagosDTO(
            tieneAtrasos = true,
            cursosAtrasados = cursos,
            mensaje = if (cursos.size == 1) "Tiene pagos atrasados en ${cursos[0].cursoNombre}"
            else "Tiene pagos atrasados en ${cursos.size} cursos"
        )
    }

    // =============================================
    // Obtener accesos recientes
    // =============================================

    @Transactional(readOnly = true)
    fun getAccesosRecientes(limit: Int): List<AccesoDTO> {
        return accesoRepository
            .findAllByOrderByFechaHoraDesc(PageRequest.of(0, limit.coerceAtLeast(1)))
            .map { mapAccesoToDTO(it) }
    }

    // =============================================
    // Obtener accesos por usuario
    // =============================================

    @Transactional(readOnly = true)
    fun getAccesosPorUsuario(usuarioId: Long, dias: Int): List<AccesoDTO> {
        val fechaLimite = AppTime.ahora().minusDays(dias.toLong())

        return accesoRepository
            .findByUsuarioIdAndFechaHoraAfterOrderByFechaHoraDesc(usuarioId, fechaLimite)
            .map { mapAccesoToDTO(it) }
    }

    // =============================================
    // Obtener estadísticas
    // =============================================

    @Transactional(readOnly = true)
    fun getEstadisticasAccesos(idPortero : Long? = null): EstadisticasAccesoDTO {
        val ahora = AppTime.ahora()
        val hoy = ahora.toLocalDate().atStartOfDay()
        val ultimaSemana = ahora.minusDays(7)
        val inicioMes = ahora.withDayOfMonth(1).toLocalDate().atStartOfDay()

        val totalHoy = accesoRepository.countByFechaHoraAfter(hoy).toInt()
        val totalSemana = accesoRepository.countByFechaHoraAfter(ultimaSemana).toInt()
        val totalMes = accesoRepository.countByFechaHoraAfter(inicioMes).toInt()

        val promedioDiario = if (totalMes > 0) totalMes / 30.0 else 0.0

        if (idPortero == null) {
            return EstadisticasAccesoDTO(
                totalHoy = totalHoy,
                totalSemana = totalSemana,
                totalEsteMes = totalMes,
                promedioDiario = promedioDiario,
                turnoRegistradoHoy = false,
                horaTurnoHoy = null
            )
        }

        val portero = usuarioRepository.findById(idPortero)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        if (!portero.tieneRol(RolType.PORTERIA)) {
            throw IllegalArgumentException("El usuario no tiene rol de portería")
        }

        val turnoHoy = accesoRepository.findUltimoTurnoHoy(
            idPortero,
            TipoAcceso.TURNO,
            hoy
        )

        return EstadisticasAccesoDTO(
            totalHoy = totalHoy,
            totalSemana = totalSemana,
            totalEsteMes = totalMes,
            promedioDiario = promedioDiario,
            turnoRegistradoHoy = turnoHoy != null,
            horaTurnoHoy = turnoHoy?.fechaHora
        )
    }

    // ========================================
    // MIS ACCESOS (Usuario actual)
    // ========================================

    @Transactional(readOnly = true)
    fun getMisAccesos(
        usuarioId: Long,
        page: Int,
        size: Int,
        meses: List<Int>?
    ): Page<AccesoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val pageable: Pageable = PageRequest.of(page, size)

        val accesosPage = accesoRepository.findMisAccesos(
            usuarioId = usuario.id,
            meses = meses,
            pageable = pageable
        )

        return accesosPage.map { acceso -> mapAccesoToDTO(acceso) }
    }

    // ========================================
    // TODOS LOS ACCESOS (Admin/Oficina)
    // ========================================

    @Transactional(readOnly = true)
    fun getTodosAccesos(
        usuarioId: Long,
        page: Int,
        size: Int,
        search: String?,
        roles: List<RolType>?,
        meses: List<Int>?
    ): Page<AccesoDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        // Validar permisos
        require(
            usuario.tieneRol(RolType.ADMINISTRADOR) ||
                    usuario.tieneRol(RolType.OFICINA)
        ) {
            "No tienes permisos para ver todos los accesos"
        }

        val pageable: Pageable = PageRequest.of(page, size)

        // Preparar filtros para query nativa
        val rolesFilter = if (roles.isNullOrEmpty()) null else "filter"
        val alumno = roles?.contains(RolType.ALUMNO) ?: false
        val profesor = roles?.contains(RolType.PROFESOR) ?: false
        val administrador = roles?.contains(RolType.ADMINISTRADOR) ?: false
        val oficina = roles?.contains(RolType.OFICINA) ?: false

        // Convertir meses a formato PostgreSQL array
        val mesesFilter = if (meses.isNullOrEmpty()) null else "filter"
        val mesesArray = if (meses.isNullOrEmpty()) null else "{${meses.joinToString(",")}}"

        val accesosPage = accesoRepository.findTodosAccesos(
            search = search,
            rolesFilter = rolesFilter,
            alumno = alumno,
            profesor = profesor,
            administrador = administrador,
            oficina = oficina,
            mesesFilter = mesesFilter,
            meses = mesesArray,
            pageable = pageable
        )

        return accesosPage.map { acceso -> mapAccesoToDTO(acceso) }
    }

    // ========================================
    // REGISTRAR ACCESO MANUAL
    // ========================================

    @Transactional
    fun registrarAccesoManual(
        adminId: Long,
        dto: RegistrarAccesoDTO
    ): AccesoDTO {
        val admin = usuarioRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        // Validar permisos (solo admin/oficina)
        require(
            admin.tieneRol(RolType.ADMINISTRADOR) ||
                    admin.tieneRol(RolType.OFICINA)
        ) {
            "No tienes permisos para registrar accesos"
        }

        val usuario = usuarioRepository.findById(dto.usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario a registrar no encontrado") }

        // Crear el acceso
        val acceso = Acceso(
            usuario = usuario,
            fechaHora = AppTime.ahora(),
            tipoAcceso = TipoAcceso.MANUAL
        )

        val accesoGuardado = accesoRepository.save(acceso)

        return mapAccesoToDTO(accesoGuardado)
    }

    @Transactional
    fun registrarAccesoInvitado(
        dni: String,
        nombre: String?,
        registradoPorId: Long
    ): AccesoDTO {
        // 1. Validar registrador tiene permisos
        val registrador = usuarioRepository.findById(registradoPorId)
            .orElseThrow { IllegalArgumentException("Usuario registrador no encontrado") }

        require(
            registrador.tieneRol(RolType.ADMINISTRADOR) ||
                    registrador.tieneRol(RolType.PORTERIA)
        ) {
            "No tienes permisos para registrar accesos"
        }

        // 2. Validar formato DNI
        require(dni.matches(Regex("^\\d{7,8}$"))) {
            "El DNI debe tener 7 u 8 dígitos"
        }

        // 3. Verificar si ya hay un acceso de este invitado hoy
        val accesoHoy = accesoRepository.findFirstByInvitadoDniAndFechaHoraGreaterThanEqual(
            dni,
            AppTime.hoy().atStartOfDay()
        )

        if (accesoHoy != null) {
            throw IllegalArgumentException(
                "Ya se registró un acceso para el DNI $dni hoy a las ${accesoHoy.fechaHora.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            )
        }

        // 4. Crear acceso de invitado
        val acceso = Acceso(
            usuario = null,
            fechaHora = AppTime.ahora(),
            tipoAcceso = TipoAcceso.INVITADO,
            invitadoDni = dni,
            invitadoNombre = nombre
        )

        val accesoGuardado = accesoRepository.save(acceso)

        // 5. Retornar DTO
        return AccesoDTO(
            id = accesoGuardado.id,
            usuarioId = null,
            usuarioNombre = nombre,
            usuarioApellido = null,
            usuarioDni = dni,
            fechaHora = accesoGuardado.fechaHora,
            tipoAcceso = TipoAcceso.INVITADO,
            alertaPagos = null,
            esInvitado = true
        )
    }

    // ========================================
    // REGISTRAR TURNO (PORTERÍA)
    // ========================================

    @Transactional
    fun comenzarTurno(
        dto: RegistrarAccesoDTO
    ): AccesoDTO {
        val usuario = usuarioRepository.findById(dto.usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario a registrar no encontrado") }

        if (!usuario.tieneRol(RolType.PORTERIA)) {
            throw IllegalArgumentException("Solo los usuarios con rol PORTERIA pueden comenzar un turno")
        }

        // Crear el acceso
        val acceso = Acceso(
            usuario = usuario,
            fechaHora = AppTime.ahora(),
            tipoAcceso = TipoAcceso.TURNO
        )

        val accesoGuardado = accesoRepository.save(acceso)

        return mapAccesoToDTO(accesoGuardado)
    }

    // ========================================
    // HELPERS
    // ========================================

    private fun mapAccesoToDTO(acceso: Acceso): AccesoDTO {
        return AccesoDTO(
            id = acceso.id,
            usuarioId = acceso.usuario?.id,
            usuarioNombre = acceso.usuario?.nombre ?: acceso.invitadoNombre,
            usuarioApellido = acceso.usuario?.apellido,
            usuarioDni = acceso.usuario?.dni ?: acceso.invitadoDni ?: "",
            fechaHora = acceso.fechaHora,
            tipoAcceso = acceso.tipoAcceso,
            alertaPagos = null,
            esInvitado = acceso.tipoAcceso == TipoAcceso.INVITADO
        )
    }
}