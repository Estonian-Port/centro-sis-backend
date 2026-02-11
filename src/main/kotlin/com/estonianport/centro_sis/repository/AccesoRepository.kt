package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Acceso
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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
        JOIN usuario u ON u.id = a.usuario_id
        WHERE (CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(u.nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR LOWER(CAST(u.apellido AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(u.dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        )
        AND (CAST(:rolesFilter AS TEXT) IS NULL OR EXISTS (
            SELECT 1 FROM rol r 
            WHERE r.usuario_id = u.id 
            AND (
                (:alumno = TRUE AND r.rol_type = 'ALUMNO')
                OR (:profesor = TRUE AND r.rol_type = 'PROFESOR')
                OR (:administrador = TRUE AND r.rol_type = 'ADMINISTRADOR')
                OR (:oficina = TRUE AND r.rol_type = 'OFICINA')
            )
        ))
        AND (CAST(:mesesFilter AS TEXT) IS NULL OR EXTRACT(MONTH FROM a.fecha_hora) = ANY(CAST(:meses AS INTEGER[])))
        ORDER BY a.fecha_hora DESC
        """,
        countQuery = """
        SELECT COUNT(a.id) FROM acceso a
        JOIN usuario u ON u.id = a.usuario_id
        WHERE (CAST(:search AS TEXT) IS NULL 
            OR LOWER(CAST(u.nombre AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR LOWER(CAST(u.apellido AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
            OR CAST(u.dni AS TEXT) LIKE '%' || CAST(:search AS TEXT) || '%'
        )
        AND (CAST(:rolesFilter AS TEXT) IS NULL OR EXISTS (
            SELECT 1 FROM rol r 
            WHERE r.usuario_id = u.id 
            AND (
                (:alumno = TRUE AND r.rol_type = 'ALUMNO')
                OR (:profesor = TRUE AND r.rol_type = 'PROFESOR')
                OR (:administrador = TRUE AND r.rol_type = 'ADMINISTRADOR')
                OR (:oficina = TRUE AND r.rol_type = 'OFICINA')
            )
        ))
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

}