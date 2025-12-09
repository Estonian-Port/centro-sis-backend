package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.repository.CursoRepository
import com.estonianport.centro_sis.repository.InscripcionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CursoService : GenericServiceImpl<Curso, Long>() {

    @Autowired
    lateinit var cursoRepository: CursoRepository

    @Autowired
    lateinit var inscripcionRepository: InscripcionRepository

    override val dao: CursoRepository
        get() = cursoRepository

    override fun delete(id: Long) {
        val curso : Curso = cursoRepository.findById(id).get()
        curso.fechaBaja = LocalDate.now()
        cursoRepository.save(curso)
    }

    fun getAllCursosByAlumnoId(id: Long): List<Curso> {
        return inscripcionRepository.getAllInscripcionesByUsuarioId(id)
            .map { it.curso }
    }

    fun cantAlumnosInscritos(cursoId: Long): Int {
        return inscripcionRepository.countByCursoIdAndFechaBajaIsNull(cursoId)
    }

    fun countCursos(): Long {
        return cursoRepository.countByFechaBajaIsNull()
    }

    fun getById(id: Long): Curso {
        return cursoRepository.findById(id).orElseThrow { Exception("Curso no encontrado") }
    }

    fun getAllCursos(): List<Curso> {
        return cursoRepository.findAll().filter { it.fechaBaja == null }
    }

    fun alta (nuevoCurso : Curso) : Curso {
        return cursoRepository.save(nuevoCurso)
    }
}