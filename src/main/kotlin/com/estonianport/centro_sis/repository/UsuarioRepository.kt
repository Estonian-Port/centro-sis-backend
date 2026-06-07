package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : CrudRepository<Usuario, Long> {

    fun findOneByEmail(email: String): Usuario?

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol r
        WHERE u.id = :id
        AND (r IS NULL OR r.fechaBaja IS NULL)
    """)
    override fun findById(@Param("id") id: Long): Optional<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol r
        WHERE u.fechaBaja IS NULL
        AND u.id != :userId
        AND (r IS NULL OR r.fechaBaja IS NULL)
        ORDER BY u.nombre ASC
    """)
    fun getAllActivosExcepto(@Param("userId") userId: Long): Set<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol r
        WHERE u.fechaBaja IS NULL
        AND (r IS NULL OR r.fechaBaja IS NULL)
        ORDER BY u.nombre ASC
    """)
    fun findAllActivos(): Set<Usuario>

    fun getUsuarioByEmail(email: String): Usuario?

    fun existsByDni(dni: String): Boolean

    // ─── Por rol ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findProfesores(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findAlumnos(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAdmin
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findAdministradores(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolOficina
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findOficina(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolPorteria
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findPorteria(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol
        WHERE u.id IN :ids
    """)
    fun findAllWithRolesByIds(@Param("ids") ids: List<Long>): List<Usuario>

    // ─── Búsqueda fulltext en DB (evita traer todo y filtrar en Kotlin) ───────

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
        AND r.fechaBaja IS NULL
        AND u.estado IN ('ACTIVO', 'INACTIVO')
        AND (
            LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.dni)      LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY u.nombre ASC
    """)
    fun searchAlumnos(@Param("q") q: String): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
        AND r.fechaBaja IS NULL
        AND u.estado IN ('ACTIVO', 'INACTIVO')
        AND (
            LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.dni)      LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY u.nombre ASC
    """)
    fun searchProfesores(@Param("q") q: String): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol r
        WHERE u.fechaBaja IS NULL
        AND u.estado IN ('ACTIVO', 'INACTIVO')
        AND (r IS NULL OR r.fechaBaja IS NULL)
        AND (
            LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.dni)      LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY u.nombre ASC
    """)
    fun searchTodos(@Param("q") q: String): List<Usuario>
}