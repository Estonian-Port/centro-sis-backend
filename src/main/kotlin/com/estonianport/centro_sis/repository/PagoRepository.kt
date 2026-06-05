package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Pago
import com.estonianport.centro_sis.model.PagoComision
import com.estonianport.centro_sis.model.PagoCurso
import com.estonianport.centro_sis.model.PagoAlquiler
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PagoRepository : JpaRepository<Pago, Long> {

    // ========================================
    // OBTENER IDS PAGINADOS - Admin
    // ========================================

    @Query("""
        SELECT p.id FROM Pago p
        WHERE p.fechaBaja IS NULL
        AND (
            (TYPE(p) = com.estonianport.centro_sis.model.PagoCurso 
             AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoCurso pc 
                WHERE pc.id = p.id
                AND (pc.inscripcion.curso.nombre LIKE %:search%
                     OR pc.inscripcion.alumno.usuario.nombre LIKE %:search%
                     OR pc.inscripcion.alumno.usuario.apellido LIKE %:search%)
            )))
            OR
            (TYPE(p) = com.estonianport.centro_sis.model.PagoAlquiler
             AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoAlquiler pa
                WHERE pa.id = p.id
                AND (pa.curso.nombre LIKE %:search%
                     OR pa.profesor.usuario.nombre LIKE %:search%
                     OR pa.profesor.usuario.apellido LIKE %:search%)
            )))
            OR
            (TYPE(p) = com.estonianport.centro_sis.model.PagoMatricula
             AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoMatricula pm
                WHERE pm.id = p.id
                AND (pm.alumno.usuario.nombre LIKE %:search%
                     OR pm.alumno.usuario.apellido LIKE %:search%)
            )))
        )
        AND (:tipos IS NULL OR TYPE(p) IN :tipos)
        AND (:meses IS NULL OR EXTRACT(MONTH FROM p.fecha) IN :meses)
    """)
    fun findRecibidosIdsForAdmin(
        @Param("search") search: String?,
        @Param("tipos") tipos: List<Class<out Pago>>?,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Long>

    // ========================================
    // OBTENER IDS PAGINADOS - Profesor
    // ========================================

    @Query("""
        SELECT p.id FROM Pago p
        WHERE p.fechaBaja IS NULL
        AND (
            (TYPE(p) = com.estonianport.centro_sis.model.PagoCurso
             AND EXISTS (
                SELECT 1 FROM PagoCurso pc
                INNER JOIN pc.inscripcion i
                INNER JOIN i.curso c
                INNER JOIN c.profesores prof
                WHERE pc.id = p.id
                AND prof.id = :profesorId
                AND c.tipoCurso = com.estonianport.centro_sis.model.enums.CursoType.ALQUILER
            )
            AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoCurso pc
                WHERE pc.id = p.id
                AND (pc.inscripcion.curso.nombre LIKE %:search%
                     OR pc.inscripcion.alumno.usuario.nombre LIKE %:search%
                     OR pc.inscripcion.alumno.usuario.apellido LIKE %:search%)
            )))
            OR
            (TYPE(p) = com.estonianport.centro_sis.model.PagoComision
             AND EXISTS (
                SELECT 1 FROM PagoComision pcm
                WHERE pcm.id = p.id
                AND pcm.profesor.id = :profesorId
            )
            AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoComision pcm
                WHERE pcm.id = p.id
                AND (pcm.curso.nombre LIKE %:search%
                     OR pcm.profesor.usuario.nombre LIKE %:search%
                     OR pcm.profesor.usuario.apellido LIKE %:search%)
            )))
        )
        AND (:tipos IS NULL OR TYPE(p) IN :tipos)
        AND (:meses IS NULL OR EXTRACT(MONTH FROM p.fecha) IN :meses)
    """)
    fun findRecibidosIdsForProfesor(
        @Param("profesorId") profesorId: Long,
        @Param("search") search: String?,
        @Param("tipos") tipos: List<Class<out Pago>>?,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Long>

    // ========================================
    // OBTENER IDS PAGINADOS - Realizados (Admin)
    // ========================================

    @Query("""
        SELECT p.id FROM Pago p
        WHERE p.fechaBaja IS NULL
        AND TYPE(p) = com.estonianport.centro_sis.model.PagoComision
        AND (:search IS NULL OR EXISTS (
            SELECT 1 FROM PagoComision pcm
            WHERE pcm.id = p.id
            AND (pcm.curso.nombre LIKE %:search%
                 OR pcm.profesor.usuario.nombre LIKE %:search%
                 OR pcm.profesor.usuario.apellido LIKE %:search%)
        ))
        AND (:meses IS NULL OR EXTRACT(MONTH FROM p.fecha) IN :meses)
    """)
    fun findRealizadosIdsForAdmin(
        @Param("search") search: String?,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Long>

    // ========================================
    // OBTENER IDS PAGINADOS - Realizados (Profesor)
    // ========================================

    @Query("""
        SELECT p.id FROM Pago p
        WHERE p.fechaBaja IS NULL
        AND TYPE(p) = com.estonianport.centro_sis.model.PagoAlquiler
        AND EXISTS (
            SELECT 1 FROM PagoAlquiler pa
            WHERE pa.id = p.id
            AND pa.profesor.id = :profesorId
        )
        AND (:search IS NULL OR EXISTS (
            SELECT 1 FROM PagoAlquiler pa
            WHERE pa.id = p.id
            AND pa.curso.nombre LIKE %:search%
        ))
        AND (:meses IS NULL OR EXTRACT(MONTH FROM p.fecha) IN :meses)
    """)
    fun findRealizadosIdsForProfesor(
        @Param("profesorId") profesorId: Long,
        @Param("search") search: String?,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Long>

    // ========================================
    // OBTENER IDS PAGINADOS - Realizados (Alumno)
    // ========================================

    @Query("""
        SELECT p.id FROM Pago p
        WHERE p.fechaBaja IS NULL
        AND (
            (TYPE(p) = com.estonianport.centro_sis.model.PagoCurso
             AND EXISTS (
                SELECT 1 FROM PagoCurso pc
                WHERE pc.id = p.id
                AND pc.inscripcion.alumno.id = :alumnoId
             )
             AND (:search IS NULL OR EXISTS (
                SELECT 1 FROM PagoCurso pc
                WHERE pc.id = p.id
                AND pc.inscripcion.curso.nombre LIKE %:search%
             )))
            OR
            (TYPE(p) = com.estonianport.centro_sis.model.PagoMatricula
             AND :search IS NULL
             AND EXISTS (
                SELECT 1 FROM PagoMatricula pm
                WHERE pm.id = p.id
                AND pm.alumno.id = :alumnoId
             ))
        )
        AND (:meses IS NULL OR EXTRACT(MONTH FROM p.fecha) IN :meses)
    """)
    fun findRealizadosIdsForAlumno(
        @Param("alumnoId") alumnoId: Long,
        @Param("search") search: String?,
        @Param("meses") meses: List<Int>?,
        pageable: Pageable
    ): Page<Long>


    // ========================================
    // HIDRATACIÓN DE IDENTIFICADORES CON GRAPH
    // ========================================

    @Query("""
        SELECT DISTINCT p FROM Pago p
        LEFT JOIN FETCH p.registradoPor
        WHERE p.id IN :ids
        ORDER BY p.id
    """)
    fun findWithGraphByIds(@Param("ids") ids: List<Long>): List<Pago>


    // ========================================
    // MÉTODOS AUXILIARES ESPECÍFICOS
    // ========================================

    @Query("""
        SELECT pcm FROM PagoComision pcm
        LEFT JOIN FETCH pcm.curso cc
        LEFT JOIN FETCH pcm.profesor prc
        LEFT JOIN FETCH prc.usuario uc
        WHERE cc.id = :cursoId 
        AND prc.usuario.id = :profesorUsuarioId
        AND pcm.fechaBaja IS NULL
    """)
    fun findUltimoPagoComisionActivo(
        @Param("cursoId") cursoId: Long,
        @Param("profesorUsuarioId") profesorUsuarioId: Long
    ): PagoComision?

    @Query("""
        SELECT pcm FROM PagoComision pcm
        LEFT JOIN FETCH pcm.curso cc
        LEFT JOIN FETCH pcm.profesor prc
        LEFT JOIN FETCH prc.usuario uc
        WHERE cc.id = :cursoId 
        AND pcm.fechaBaja IS NULL
    """)
    fun findComisionesByCursoId(@Param("cursoId") cursoId: Long): List<PagoComision>
}