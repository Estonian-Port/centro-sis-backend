package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {

    fun countByFechaBajaIsNull(): Long


    @Query("SELECT c FROM Curso c WHERE c.fechaBaja IS NULL ORDER BY c.nombre ASC")
    fun findAllActivosOrdenados(): List<Curso>

}