package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Usuario
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : CrudRepository<Usuario, Long> {

    fun findOneByEmail(email: String): Usuario?

    @Query("""
        SELECT DISTINCT u FROM Usuario u 
        LEFT JOIN FETCH u.listaRol r 
        WHERE u.fechaBaja IS NULL 
        AND u.id != :userId 
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun getAllActivosExcepto(userId: Long): Set<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u 
        LEFT JOIN FETCH u.listaRol r 
        WHERE u.fechaBaja IS NULL 
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findAllActivos(): Set<Usuario>

    fun getUsuarioByEmail(email: String): Usuario?

    fun existsByDni(dni: String): Boolean

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
    AND r.fechaBaja IS NULL
    ORDER BY u.nombre ASC
"""
    )
    fun findProfesores(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
    AND r.fechaBaja IS NULL
    ORDER BY u.nombre ASC
"""
    )
    fun findAlumnos(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAdmin
    AND r.fechaBaja IS NULL
    ORDER BY u.nombre ASC
"""
    )
    fun findAdministradores(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolOficina
    AND r.fechaBaja IS NULL
    ORDER BY u.nombre ASC
"""
    )
    fun findOficina(): List<Usuario>

    @Query(
        """
    SELECT DISTINCT u FROM Usuario u
    INNER JOIN u.listaRol r
    WHERE TYPE(r) = com.estonianport.centro_sis.model.RolPorteria
    AND r.fechaBaja IS NULL
    ORDER BY u.nombre ASC
"""
    )
    fun findPorteria(): List<Usuario>
    
    override fun findById(id: Long): Optional<Usuario>
}