package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {
    @Query(
        """
        SELECT r.curso
        FROM Usuario u
        INNER JOIN u.listaRol r
        WHERE u.id = :id 
        AND r.fechaBaja IS NULL
        """
    )

    fun getAllByUsuarioId(id: Long): List<Curso>

}