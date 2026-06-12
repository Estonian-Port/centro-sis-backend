package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.response.UsuarioDetailResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.model.enums.RolType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UsuarioDetalleFacade(
    private val usuarioService: UsuarioService,
    private val cursoService: CursoService,
    private val inscripcionService: InscripcionService
) {
    
    fun obtenerDetalleUsuario(usuarioId: Long): UsuarioDetailResponseDto {
        val usuario = usuarioService.getById(usuarioId)

        val cursosDictados = if (usuario.tieneRol(RolType.PROFESOR)) {
            cursoService.obtenerCursosProfesorId(usuarioId)
        } else emptyList()

        val cursosInscriptos = if (usuario.tieneRol(RolType.ALUMNO)) {
            inscripcionService.obtenerInscripcionesPorAlumno(usuarioId)
                .map { CursoMapper.buildCursoAlumnoResponseDto(it) }
        } else emptyList()

        return UsuarioMapper.buildUsuarioDetailDto(usuario, cursosInscriptos, cursosDictados)
    }
}