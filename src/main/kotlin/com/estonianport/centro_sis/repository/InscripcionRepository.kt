package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Inscripcion
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InscripcionRepository : CrudRepository<Inscripcion, Long> {

    @Query(
        """
    SELECT i
    FROM Inscripcion i
    JOIN FETCH i.curso c
    JOIN i.alumno a
    JOIN a.usuario u
    WHERE u.id = :id 
    AND i.fechaBaja IS NULL
    AND a.fechaBaja IS NULL
    ORDER BY c.nombre
    """
    )
    fun getAllInscripcionesByUsuarioId(id: Long): List<Inscripcion>

    fun countByCursoIdAndFechaBajaIsNull(cursoId: Long): Int

}