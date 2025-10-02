package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.response.EstadisticasResponseDto
import org.springframework.stereotype.Service

@Service
class AdministracionService(
    private val usuarioService: UsuarioService,
) {

    fun verificarRol(usuarioId: Long) {
        val usuario = usuarioService.findById(usuarioId)
            ?: throw NotFoundException("Usuario no encontrado con ID: $usuarioId")

        if (!usuario.esAdministrador) {
            throw IllegalAccessException("El usuario no tiene permisos de administrador")
        }
    }

    fun getEstadisticas(): EstadisticasResponseDto {
        return EstadisticasResponseDto(
            totalUsuarios = usuarioService.totalUsuarios(),
            usuariosActivos = usuarioService.countUsuariosActivos(),
            usuariosPendientes = usuarioService.countUsuariosPendientes(),
            usuariosInactivos = usuarioService.countUsuariosInactivos(),
        )
    }

}