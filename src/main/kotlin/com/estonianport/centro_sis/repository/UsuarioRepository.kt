package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.dto.response.UsuarioAccesoView
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoPagoType
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface UsuarioRepository : JpaRepository<Usuario, Long> {

    @Query("""
    SELECT DISTINCT u FROM Usuario u
    LEFT JOIN FETCH u.listaRol r
    WHERE u.email = :email
    AND (r IS NULL OR r.fechaBaja IS NULL)
""")
    fun findOneByEmail(@Param("email") email: String): Usuario?

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
    WHERE u.email = :email
    AND (r IS NULL OR r.fechaBaja IS NULL)
""")
    fun getUsuarioByEmail(@Param("email") email: String): Usuario?

    fun existsByDni(dni: String): Boolean

    fun existsByDniAndIdNot(dni: String, id: Long): Boolean

    // ─── Por rol ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        LEFT JOIN FETCH u.listaRol
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findProfesores(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        LEFT JOIN FETCH u.listaRol
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findAlumnos(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        LEFT JOIN FETCH u.listaRol
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAdmin
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findAdministradores(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        LEFT JOIN FETCH u.listaRol
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolOficina
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findOficina(): List<Usuario>

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        INNER JOIN u.listaRol r
        LEFT JOIN FETCH u.listaRol
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolPorteria
        AND r.fechaBaja IS NULL
        ORDER BY u.nombre ASC
    """)
    fun findPorteria(): List<Usuario>

    // ─── Búsqueda fulltext en DB ───────────────────────────────────────────────

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

    // ─── PAGINACIÓN ───────────────────────────────────────────────────────────

    /**
     * IDs paginados de todos los usuarios activos (excepto el que consulta),
     * con filtro de búsqueda opcional por nombre/apellido/dni/email.
     * Los roles se filtran en memoria sobre el resultado pequeño (evita
     * el producto cartesiano que rompía la paginación).
     */
    @Query(
        value = """
            SELECT u.id FROM Usuario u
            WHERE u.fechaBaja IS NULL
            AND u.id != :userId
            AND (:search IS NULL OR :search = '' OR
                LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.apellido) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.dni)      LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
            )
            ORDER BY u.nombre ASC
        """,
        countQuery = """
            SELECT COUNT(u.id) FROM Usuario u
            WHERE u.fechaBaja IS NULL
            AND u.id != :userId
            AND (:search IS NULL OR :search = '' OR
                LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.apellido) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.dni)      LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
            )
        """
    )
    fun findIdsActivosExcepto(
        @Param("userId") userId: Long,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Long>

    /**
     * Segunda pasada: hidrata los usuarios de la página con sus roles en una
     * sola query (evita N+1).
     */
    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.listaRol r
        WHERE u.id IN :ids
        AND (r IS NULL OR r.fechaBaja IS NULL)
    """)
    fun findWithRolesByIds(@Param("ids") ids: List<Long>): List<Usuario>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.ultimoIngresoAlSistema = :fecha WHERE u.email = :email")
    fun updateUltimoIngresoByEmail(@Param("email") email: String, @Param("fecha") fecha: LocalDateTime): Int

    @Query("""
    SELECT u.id AS id, u.nombre AS nombre, u.apellido AS apellido, u.dni AS dni, u.estado AS estado
    FROM Usuario u WHERE u.id = :id
""")
    fun findAccesoViewById(@Param("id") id: Long): UsuarioAccesoView?

}