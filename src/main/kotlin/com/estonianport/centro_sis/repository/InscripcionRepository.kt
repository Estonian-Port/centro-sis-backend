package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Inscripcion
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InscripcionRepository : CrudRepository<Inscripcion, Long> {

    fun countByCursoIdAndFechaBajaIsNull(cursoId: Long): Int

    fun findAllByCursoIdAndFechaBajaIsNull(cursoId: Long): List<Inscripcion>
}