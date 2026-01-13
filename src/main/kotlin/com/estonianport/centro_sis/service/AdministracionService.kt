package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.response.EstadisticasResponseDto
import com.estonianport.centro_sis.model.enums.RolType
import org.springframework.stereotype.Service

@Service
class AdministracionService(
    private val usuarioService: UsuarioService,
    private val cursoService: CursoService,
    private val pagoService: PagoService
) {

    fun verificarRol(usuarioId: Long) {
        val usuario = usuarioService.findById(usuarioId)
            ?: throw NotFoundException("Usuario no encontrado con ID: $usuarioId")

        if (!usuario.tieneRol(RolType.ADMINISTRADOR)) {
            throw IllegalAccessException("El usuario no tiene permisos de administrador")
        }
    }

    fun getEstadisticas(): EstadisticasResponseDto {
        val totalAlumnosActivos = usuarioService.countAlumnosActivos()
        val totalCursos = cursoService.countCursos()
        val totalProfesores = usuarioService.countProfesores()
        val ingresosMes = pagoService.calcularIngresosMensuales()

        return EstadisticasResponseDto(
            alumnosActivos = totalAlumnosActivos,
            cursos = totalCursos,
            profesores = totalProfesores,
            ingresosMensuales = ingresosMes
        )
    }

}