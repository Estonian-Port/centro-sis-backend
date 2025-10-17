package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {
}