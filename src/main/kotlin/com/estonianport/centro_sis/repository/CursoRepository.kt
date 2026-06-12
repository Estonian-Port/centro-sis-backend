package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.ParteAsistencia
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {

    fun countByFechaBajaIsNull(): Long

    /**
     * Lista principal de cursos (sin paginación): trae solo lo necesario para armar
     * CursoResponseDto. Las inscripciones se cargan aparte con
     * findCursosConInscripcionesByIdsIn para evitar el producto cartesiano.
     */
    @Query("""
        SELECT c FROM Curso c
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.tiposPago
        WHERE c.fechaBaja IS NULL
        ORDER BY
            CASE c.estadoAlta
                WHEN 'ACTIVO'    THEN 0
                WHEN 'PENDIENTE' THEN 1
                WHEN 'BAJA'      THEN 2
                ELSE 3
            END ASC,
            CASE
                WHEN c.estadoAlta = 'BAJA'         THEN 2
                WHEN CURRENT_DATE < c.fechaInicio  THEN 1
                WHEN CURRENT_DATE > c.fechaFin     THEN 2
                ELSE 0
            END ASC,
            c.nombre ASC
    """)
    fun findAllOrdenadosConDetalles(): List<Curso>

    /**
     * ─── PAGINACIÓN (pantalla de administración) ──────────────────────────────
     *
     * Paso 1: obtener solo los IDs con la paginación y los filtros aplicados.
     * Usar IDs evita el producto cartesiano que rompía el conteo de páginas
     * cuando se combinaba JOIN FETCH con Pageable.
     */
    @Query(
        value = """
            SELECT c.id FROM Curso c
            WHERE c.fechaBaja IS NULL
            AND (:search IS NULL OR :search = '' OR
                LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:estadoAlta IS NULL OR c.estadoAlta = :estadoAlta)
            ORDER BY
                CASE c.estadoAlta
                    WHEN 'ACTIVO'    THEN 0
                    WHEN 'PENDIENTE' THEN 1
                    WHEN 'BAJA'      THEN 2
                    ELSE 3
                END ASC,
                CASE
                    WHEN c.estadoAlta = 'BAJA'         THEN 2
                    WHEN CURRENT_DATE < c.fechaInicio  THEN 1
                    WHEN CURRENT_DATE > c.fechaFin     THEN 2
                    ELSE 0
                END ASC,
                c.nombre ASC
        """,
        countQuery = """
            SELECT COUNT(c.id) FROM Curso c
            WHERE c.fechaBaja IS NULL
            AND (:search IS NULL OR :search = '' OR
                LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:estadoAlta IS NULL OR c.estadoAlta = :estadoAlta)
        """
    )
    fun findIdsPaginados(
        @Param("search") search: String?,
        @Param("estadoAlta") estadoAlta: EstadoType?,
        pageable: Pageable
    ): Page<Long>

    /**
     * Paso 2: hidratar los cursos de la página con todas sus relaciones en
     * una sola query (evita N+1). Las inscripciones se cargan por separado
     * en findCursosConInscripcionesByIdsIn.
     */
    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.tiposPago
        WHERE c.id IN :ids
    """)
    fun findConDetallesByIds(@Param("ids") ids: List<Long>): List<Curso>

    // ─── Queries existentes (sin cambios) ─────────────────────────────────────

    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.inscripciones i
        LEFT JOIN FETCH i.alumno a
        LEFT JOIN FETCH a.usuario
        WHERE c.id IN :ids
    """)
    fun findCursosConInscripcionesByIdsIn(@Param("ids") ids: List<Long>): List<Curso>

    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.tiposPago
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario
        WHERE c.id = :id
    """)
    fun findByIdConDetalles(@Param("id") id: Long): Optional<Curso>

    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.inscripciones i
        LEFT JOIN FETCH i.alumno a
        LEFT JOIN FETCH a.usuario
        WHERE c.id = :id
    """)
    fun findInscripcionesByCursoId(@Param("id") id: Long): Optional<Curso>

    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.tiposPago
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario
        JOIN c.profesores p2
        WHERE p2.usuario.id = :idProfe
        AND c.fechaBaja IS NULL
    """)
    fun findCursosActivosPorProfesorId(@Param("idProfe") idProfe: Long): List<Curso>

    @Query("SELECT pa FROM ParteAsistencia pa WHERE pa.curso.id = :cursoId ORDER BY pa.fecha DESC")
    fun findPartesAsistenciaByCursoId(@Param("cursoId") cursoId: Long): List<ParteAsistencia>

    @Modifying
    @Query("UPDATE Curso c SET c.nombre = :nombre WHERE c.id = :id")
    fun updateNombre(@Param("id") id: Long, @Param("nombre") nombre: String): Int

    @Modifying
    @Query("""
        UPDATE Curso c
        SET c.fechaBaja = CURRENT_DATE, c.estadoAlta = 'BAJA'
        WHERE c.id = :id AND c.fechaBaja IS NULL
    """)
    fun darDeBajaDirecto(@Param("id") id: Long): Int
}
