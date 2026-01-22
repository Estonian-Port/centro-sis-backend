package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.AccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoDTO
import com.estonianport.centro_sis.model.Acceso
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
    private val usuarioRepository: UsuarioRepository
) {

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
    // REGISTRAR ACCESO QR (futuro)
    // ========================================

    @Transactional
    fun registrarAccesoQR(usuarioId: Long): AccesoDTO {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        // Crear el acceso
        val acceso = Acceso(
            usuario = usuario,
            fechaHora = LocalDateTime.now(),
            tipoAcceso = TipoAcceso.QR
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
            tipoAcceso = acceso.tipoAcceso
        )
    }
}