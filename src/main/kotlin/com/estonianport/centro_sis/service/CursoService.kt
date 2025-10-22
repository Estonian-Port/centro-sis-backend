package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.repository.CursoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CursoService : GenericServiceImpl<Curso, Long>() {

    @Autowired
    lateinit var cursoRepository: CursoRepository

    override val dao: CursoRepository
        get() = cursoRepository

    override fun delete(id: Long) {
        val curso : Curso = cursoRepository.findById(id).get()
        curso.fechaBaja = LocalDate.now()
        cursoRepository.save(curso)
    }

    //cursoRepository.getCursosByUsuario
}