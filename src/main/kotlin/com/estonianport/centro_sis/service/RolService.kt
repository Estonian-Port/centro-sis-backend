package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.repository.RolRepository
import org.springframework.stereotype.Service

@Service
class RolService(private val rolRepository: RolRepository) {

    fun getProfesorByCursoId(cursoId: Long): List<Usuario> {
        val roles = rolRepository.findRolProfesorByCurso_Id(cursoId)
        return roles.map {it.usuario}
    }
}