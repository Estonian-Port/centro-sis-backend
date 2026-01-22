package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.ParteAsistencia
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {

    fun countByFechaBajaIsNull(): Long


    @Query("""
    SELECT c FROM Curso c
    ORDER BY
        CASE c.estadoAlta
            WHEN 'ACTIVO' THEN 0
            WHEN 'PENDIENTE' THEN 1
            WHEN 'BAJA' THEN 2
            ELSE 3
        END ASC,
        CASE
            WHEN c.estadoAlta = 'BAJA' THEN 2
            WHEN CURRENT_DATE < c.fechaInicio THEN 1
            WHEN CURRENT_DATE > c.fechaFin THEN 2
            ELSE 0
        END ASC,
        c.nombre ASC
""")
    fun findAllOrdenados(): List<Curso>



    @Query("SELECT pa FROM ParteAsistencia pa WHERE pa.curso.id = :cursoId")
    fun findPartesAsistenciaByCursoId(cursoId: Long): List<ParteAsistencia>

}