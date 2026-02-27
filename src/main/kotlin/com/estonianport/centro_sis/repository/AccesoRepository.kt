package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Acceso
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AccesoRepository : JpaRepository<Acceso, Long> {

    // ========================================
    // MIS ACCESOS (Usuario actual)
    // ========================================

    /**
     * Obtener accesos del usuario actual paginados
     */
    @Query(
        """
        SELECT a FROM Acceso a
        WHERE a.usuario.id = :usuarioId
        AND (:meses IS NULL OR MONTH(a.fechaHora) IN :meses)
        ORDER BY a.fechaHora DESC
    """
    )
    fun findMisAccesos(
        @Param("usuarioId") usuarioId: Long,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Acceso>

    // ========================================
    // TODOS LOS ACCESOS (Admin/Oficina)
    // ========================================

    /**
     * Obtener todos los accesos con filtros - QUERY NATIVA SQL
     */
    @Query(
        value = """
    SELECT a.* FROM acceso a
    LEFT JOIN usuario u ON u.id = a.usuario_id
    WHERE (
        -- Búsqueda en usuarios registrados
        (a.usuario_id IS NOT NULL AND (
            CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(u.nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR LOWER(CAST(u.apellido AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(u.dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        ))
        -- ✅ NUEVO: Búsqueda en invitados
        OR (a.usuario_id IS NULL AND (
            CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(a.invitado_nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(a.invitado_dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        ))
    )
    AND (
        -- Filtro de roles (solo aplica a usuarios registrados)
        a.usuario_id IS NULL  -- ✅ Invitados siempre pasan el filtro
        OR CAST(:rolesFilter AS TEXT) IS NULL 
        OR EXISTS (
            SELECT 1 FROM rol r 
            WHERE r.usuario_id = u.id 
            AND (
                (:alumno = TRUE AND r.rol_type = 'ALUMNO')
                OR (:profesor = TRUE AND r.rol_type = 'PROFESOR')
                OR (:administrador = TRUE AND r.rol_type = 'ADMINISTRADOR')
                OR (:oficina = TRUE AND r.rol_type = 'OFICINA')
            )
        )
    )
    AND (CAST(:mesesFilter AS TEXT) IS NULL OR EXTRACT(MONTH FROM a.fecha_hora) = ANY(CAST(:meses AS INTEGER[])))
    ORDER BY a.fecha_hora DESC
    """,
        countQuery = """
    SELECT COUNT(a.id) FROM acceso a
    LEFT JOIN usuario u ON u.id = a.usuario_id
    WHERE (
        (a.usuario_id IS NOT NULL AND (
            CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(u.nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR LOWER(CAST(u.apellido AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(u.dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        ))
        OR (a.usuario_id IS NULL AND (
            CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(a.invitado_nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(a.invitado_dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        ))
    )
    AND (
        a.usuario_id IS NULL
        OR CAST(:rolesFilter AS TEXT) IS NULL 
        OR EXISTS (
            SELECT 1 FROM rol r 
            WHERE r.usuario_id = u.id 
            AND (
                (:alumno = TRUE AND r.rol_type = 'ALUMNO')
                OR (:profesor = TRUE AND r.rol_type = 'PROFESOR')
                OR (:administrador = TRUE AND r.rol_type = 'ADMINISTRADOR')
                OR (:oficina = TRUE AND r.rol_type = 'OFICINA')
            )
        )
    )
    AND (CAST(:mesesFilter AS TEXT) IS NULL OR EXTRACT(MONTH FROM a.fecha_hora) = ANY(CAST(:meses AS INTEGER[])))
    """,
        nativeQuery = true
    )
    fun findTodosAccesos(
        @Param("search") search: String?,
        @Param("rolesFilter") rolesFilter: String?,
        @Param("alumno") alumno: Boolean,
        @Param("profesor") profesor: Boolean,
        @Param("administrador") administrador: Boolean,
        @Param("oficina") oficina: Boolean,
        @Param("mesesFilter") mesesFilter: String?,
        @Param("meses") meses: String?,
        pageable: Pageable
    ): Page<Acceso>

    // Buscar accesos de invitados por DNI
    fun findByInvitadoDni(dni: String): List<Acceso>

    // Contar visitas de un invitado
    fun countByInvitadoDni(dni: String): Long

    // Buscar accesos de invitados en un rango de fechas
    @Query("SELECT a FROM Acceso a WHERE a.invitadoDni = :dni AND a.fechaHora BETWEEN :desde AND :hasta")
    fun findInvitadoAccesosByFechas(
        @Param("dni") dni: String,
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime
    ): List<Acceso>

}