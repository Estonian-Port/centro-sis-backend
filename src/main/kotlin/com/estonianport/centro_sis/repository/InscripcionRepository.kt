package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.PagoCurso
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InscripcionRepository : CrudRepository<Inscripcion, Long> {

    // Trae la inscripción junto con el curso y el alumno en una sola query optimizada
    @Query("SELECT i FROM Inscripcion i LEFT JOIN FETCH i.curso LEFT JOIN FETCH i.alumno WHERE i.id = :id")
    fun findByIdWithCursoAndAlumno(@Param("id") id: Long): Optional<Inscripcion>

    // Directamente delega la consulta a la base de datos para traer solo los pagos del alumno específico
    @Query("SELECT p FROM Inscripcion i JOIN i.pagos p WHERE i.alumno.id = :alumnoId")
    fun findAllPagosByAlumnoId(@Param("alumnoId") alumnoId: Long): List<PagoCurso>

    @Deprecated("Reemplazado por findInscripcionesSummaryPorAlumnoId(idAlumno)")
    @Query("""
        SELECT i FROM Inscripcion i
        JOIN FETCH i.curso c
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario 
        LEFT JOIN FETCH c.tiposPago
        JOIN FETCH i.alumno a
        JOIN FETCH a.usuario
        LEFT JOIN FETCH i.tipoPagoSeleccionado
        LEFT JOIN FETCH i.pagos
        WHERE a.usuario.id = :idAlumno
        AND i.fechaBaja IS NULL
    """)
    fun findInscripcionesActivasConDetallesPorAlumnoId(@Param("idAlumno") idAlumno: Long): Set<Inscripcion>

    @Query("""
        SELECT i FROM Inscripcion i
        JOIN FETCH i.curso c
        LEFT JOIN FETCH c.horarios
        LEFT JOIN FETCH c.profesores p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH c.tiposPago
        JOIN FETCH i.alumno a
        JOIN FETCH a.usuario
        LEFT JOIN FETCH i.tipoPagoSeleccionado
        WHERE a.usuario.id = :alumnoId
        AND i.fechaBaja IS NULL
    """)
    fun findInscripcionesSummaryPorAlumnoId(
        @Param("alumnoId") alumnoId: Long
    ): List<Inscripcion>
}