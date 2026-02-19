package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Usuario
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : CrudRepository<Usuario, Long> {

    fun findOneByEmail(email: String): Usuario?

    fun findAllByOrderByNombreAsc(): List<Usuario>

    fun getUsuarioByEmail(email: String): Usuario?

    fun existsByDni(dni: String): Boolean

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
    ORDER BY u.nombre ASC
"""
    )
    fun findProfesores(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
    ORDER BY u.nombre ASC
"""
    )
    fun findAlumnos(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAdmin
    ORDER BY u.nombre ASC
"""
    )
    fun findAdministradores(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolOficina
    ORDER BY u.nombre ASC
"""
    )
    fun findOficina(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolPorteria
    ORDER BY u.nombre ASC
"""
    )
    fun findPorteria(): List<Usuario>


    override fun findById(id: Long): Optional<Usuario>
}