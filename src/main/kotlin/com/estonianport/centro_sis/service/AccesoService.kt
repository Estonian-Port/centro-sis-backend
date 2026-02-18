package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.AccesoDTO
import com.estonianport.centro_sis.dto.AlertaPagosDTO
import com.estonianport.centro_sis.dto.CursoAtrasoDTO
import com.estonianport.centro_sis.dto.EstadisticasAccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoDTO
import com.estonianport.centro_sis.model.Acceso
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoPagoType
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.estonianport.centro_sis.repository.AccesoRepository
import com.estonianport.centro_sis.repository.UsuarioRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AccesoService(
    private val accesoRepository: AccesoRepository,
    private val usuarioRepository: UsuarioRepository,
) {

    // =============================================
    // Registrar acceso por QR
    // =============================================

    @Transactional
    fun registrarAccesoQR(
        usuarioId: Long,
        registradoPorId: Long
    ): AccesoDTO {
        // ✅ Validar registrador tiene permisos
        val registrador = usuarioRepository.findById(registradoPorId)
            .orElseThrow { IllegalArgumentException("Usuario registrador no encontrado") }

        require(
            registrador.tieneRol(RolType.ADMINISTRADOR) ||
                    registrador.tieneRol(RolType.PORTERIA)
        ) {
            "No tienes permisos para registrar accesos"
        }

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        require(usuario.estado != EstadoType.BAJA ) {
            "El usuario ${usuario.nombreCompleto()} fue dado de baja"
        }

        val ultimoAcceso = accesoRepository.findAll()
            .filter { it.usuario.id == usuarioId }
            .maxByOrNull { it.fechaHora }

        if (ultimoAcceso != null) {
            val hoy = LocalDateTime.now()
            if (ultimoAcceso.fechaHora.dayOfWeek == hoy.dayOfWeek) {
                throw IllegalArgumentException("El usuario ${usuario.nombreCompleto()} ya registró un acceso hoy")
            }
        }

        val acceso = Acceso(
            usuario = usuario,
            fechaHora = LocalDateTime.now(),
            tipoAcceso = TipoAcceso.QR  // Siempre QR
        )

        val accesoGuardado = accesoRepository.save(acceso)

        val alertaPagos = verificarPagosAtrasados(usuario)

        return AccesoDTO(
            id = accesoGuardado.id,
            usuarioId = usuario.id,
            usuarioNombre = usuario.nombre,
            usuarioApellido = usuario.apellido,
            usuarioDni = usuario.dni,
            fechaHora = accesoGuardado.fechaHora,
            tipoAcceso = TipoAcceso.QR,  // Siempre QR
            alertaPagos = alertaPagos
        )
    }

    // =============================================
    // Verificar pagos atrasados
    // =============================================

    private fun verificarPagosAtrasados(usuario: Usuario): AlertaPagosDTO? {
        if (!usuario.tieneRol(RolType.ALUMNO)) {
            return null
        }

        // Solo verificar si tiene rol alumno
        val rolAlumno = usuario.getRolAlumno()

        // Obtener inscripciones activas con pagos atrasados
        val cursosAtrasados = rolAlumno.inscripciones
            .filter {
                it.fechaBaja == null &&
                        it.estadoPago == EstadoPagoType.ATRASADO
            }
            .map { inscripcion ->
                CursoAtrasoDTO(
                    cursoNombre = inscripcion.curso.nombre,
                    cuotasAtrasadas = inscripcion.calcularCuotasAtrasadas(),
                    deudaPendiente = inscripcion.calcularDeudaPendiente()
                )
            }

        if (cursosAtrasados.isEmpty()) {
            return null
        }

        val mensaje = when {
            cursosAtrasados.size == 1 ->
                "Tiene pagos atrasados en ${cursosAtrasados[0].cursoNombre}"

            else ->
                "Tiene pagos atrasados en ${cursosAtrasados.size} cursos"
        }

        return AlertaPagosDTO(
            tieneAtrasos = true,
            cursosAtrasados = cursosAtrasados,
            mensaje = mensaje
        )
    }

    // =============================================
    // Obtener accesos recientes
    // =============================================

    @Transactional(readOnly = true)
    fun getAccesosRecientes(limit: Int): List<AccesoDTO> {
        return accesoRepository.findAll()
            .sortedByDescending { it.fechaHora }
            .take(limit)
            .map { mapAccesoToDTO(it) }
    }

    // =============================================
    // Obtener accesos por usuario
    // =============================================

    @Transactional(readOnly = true)
    fun getAccesosPorUsuario(usuarioId: Long, dias: Int): List<AccesoDTO> {
        val fechaLimite = LocalDateTime.now().minusDays(dias.toLong())

        return accesoRepository.findAll()
            .filter {
                it.usuario.id == usuarioId &&
                        it.fechaHora.isAfter(fechaLimite)
            }
            .sortedByDescending { it.fechaHora }
            .map { mapAccesoToDTO(it) }
    }

    // =============================================
    // Obtener estadísticas
    // =============================================

    @Transactional(readOnly = true)
    fun getEstadisticasAccesos(): EstadisticasAccesoDTO {
        val ahora = LocalDateTime.now()
        val hoy = ahora.toLocalDate().atStartOfDay()
        val ultimaSemana = ahora.minusDays(7)
        val inicioMes = ahora.withDayOfMonth(1).toLocalDate().atStartOfDay()

        val accesos = accesoRepository.findAll()

        val totalHoy = accesos.count { it.fechaHora.isAfter(hoy) }
        val totalSemana = accesos.count { it.fechaHora.isAfter(ultimaSemana) }
        val totalMes = accesos.count { it.fechaHora.isAfter(inicioMes) }

        val promedioDiario = if (totalMes > 0) totalMes / 30.0 else 0.0

        return EstadisticasAccesoDTO(
            totalHoy = totalHoy,
            totalSemana = totalSemana,
            totalEsteMes = totalMes,
            promedioDiario = promedioDiario,
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
            fechaHora = LocalDateTime.now(),
            tipoAcceso = TipoAcceso.MANUAL
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
            usuarioId = acceso.usuario.id,
            usuarioNombre = acceso.usuario.nombre,
            usuarioApellido = acceso.usuario.apellido,
            usuarioDni = acceso.usuario.dni,
            fechaHora = acceso.fechaHora,
            tipoAcceso = acceso.tipoAcceso,
            alertaPagos = null
        )
    }
}