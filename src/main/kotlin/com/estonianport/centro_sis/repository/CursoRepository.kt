package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.dto.response.CursoConteoAlumnosDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.PagoCurso
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
     * Lista principal de cursos (sin paginación): trae solo lo necesario para
     * armar CursoResumenDto. El conteo de alumnos se resuelve aparte con
     * countAlumnosActivosPorCursoIds (evita traer las inscripciones completas).
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
     * una sola query (evita N+1). Las inscripciones NO se incluyen acá a
     * propósito: los endpoints granulares (CursoResumenDto / CursoDetalleDto)
     * no necesitan la entidad Inscripcion completa, solo un conteo
     * (ver countAlumnosActivosPorCursoIds).
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

    @Deprecated(
        message = "No utilizar este fetch masivo de inscripciones. Utilizar countAlumnosActivosPorCursoIds para resolver el volumen de alumnos de forma agregada.",
        level = DeprecationLevel.WARNING
    )
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

    /**
     * OJO: no la deprequeé porque getById() (sin deprecar) la sigue usando
     * de verdad — tomarAsistenciaAutomatica() necesita la colección de
     * inscripciones+alumno para calcular quién estuvo presente. El resto de
     * los métodos que llaman a getById() (reemplazarProfesoresPorId,
     * actualizarHorariosCurso, etc.) NO necesitan inscripciones y están
     * pagando este fetch de más; queda como deuda técnica pre-existente,
     * fuera del alcance de esta iteración (no toco getById()).
     */
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

    /**
     * Cuenta alumnos con inscripción activa por curso, SIN traer las
     * entidades Inscripcion/Alumno/Usuario. Es lo que reemplaza la necesidad
     * de findCursosConInscripcionesByIdsIn en los listados: antes había que
     * traer todas las inscripciones de la página de cursos solo para poder
     * mostrar "cuántos alumnos tiene"; ahora es un COUNT ... GROUP BY.
     */
    @Query("""
        SELECT new  com.estonianport.centro_sis.dto.response.CursoConteoAlumnosDto(i.curso.id, COUNT(i))
        FROM Inscripcion i
        WHERE i.curso.id IN :ids AND i.estado = 'ACTIVO'
        GROUP BY i.curso.id
    """)
    fun countAlumnosActivosPorCursoIds(@Param("ids") ids: List<Long>): List<CursoConteoAlumnosDto>

    /**
     * Sub-recurso paginado de alumnos de UN curso (GET /curso/{id}/alumnos).
     * Al acotarse a un solo curso, el JOIN FETCH + Pageable ya no multiplica
     * filas entre distintos cursos de una misma página, que era lo que
     * rompía el conteo de páginas en el endpoint viejo.
     */
    @Query("""
        SELECT i FROM Inscripcion i
        JOIN FETCH i.alumno a
        JOIN FETCH a.usuario
        WHERE i.curso.id = :cursoId AND i.estado = 'ACTIVO'
        ORDER BY a.usuario.apellido ASC, a.usuario.nombre ASC
    """)
    fun findInscripcionesActivasPaginadoByCursoId(
        @Param("cursoId") cursoId: Long,
        pageable: Pageable
    ): Page<Inscripcion>

    /** Inscripción activa de UN alumno en UN curso (GET /curso/{id}/mi-inscripcion). */
    @Query("""
        SELECT i FROM Inscripcion i
        JOIN FETCH i.alumno a
        JOIN FETCH a.usuario
        WHERE i.curso.id = :cursoId AND a.id = :alumnoId AND i.estado = 'ACTIVO'
    """)
    fun findInscripcionActivaByCursoIdYAlumnoId(
        @Param("cursoId") cursoId: Long,
        @Param("alumnoId") alumnoId: Long
    ): Optional<Inscripcion>

    @Query("""
    SELECT p FROM PagoCurso p
    JOIN p.inscripcion i
    WHERE i.curso.id = :cursoId 
    AND i.alumno.id = :alumnoId 
    AND i.estado = 'ACTIVO'
    ORDER BY p.fecha DESC
""")
    fun findPagosByCursoIdYAlumnoId(
        @Param("cursoId") cursoId: Long,
        @Param("alumnoId") alumnoId: Long
    ): List<PagoCurso>
}