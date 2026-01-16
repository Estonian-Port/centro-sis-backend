package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Horario
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.TipoPago
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.repository.CursoRepository
import com.estonianport.centro_sis.repository.InscripcionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class CursoService : GenericServiceImpl<Curso, Long>() {

    @Autowired
    lateinit var cursoRepository: CursoRepository

    @Autowired
    lateinit var inscripcionRepository: InscripcionRepository

    override val dao: CursoRepository
        get() = cursoRepository

    fun countCursos(): Long {
        return cursoRepository.countByFechaBajaIsNull()
    }

    fun getById(id: Long): Curso {
        return cursoRepository.findById(id).orElseThrow { Exception("Curso no encontrado") }
    }

    fun getAllCursos(): List<Curso> {
        return cursoRepository.findAllActivosOrdenados()
    }

    fun alta(nuevoCurso: Curso): Curso {
        return cursoRepository.save(nuevoCurso)
    }

    override fun delete(id: Long) {
        val curso: Curso = cursoRepository.findById(id).get()
        curso.fechaBaja = LocalDate.now()
        cursoRepository.save(curso)
    }

    fun cantAlumnosInscriptos(cursoId: Long): Int {
        return inscripcionRepository.countByCursoIdAndFechaBajaIsNull(cursoId)
    }

    fun actualizarProfesoresDelCurso(curso: Curso, nuevosProfesores: List<RolProfesor>): Curso {
        curso.profesores.clear()
        curso.profesores.addAll(nuevosProfesores)
        return save(curso)
    }

//    fun existeCursoConNombre(nombre: String): Boolean {
//        return cursoRepository.existsByNombreAndFechaBajaIsNull(nombre)
//    }

    fun finalizarAltaCursoAlquiler(
        cursoId: Long,
        horarios: List<Horario>,
        tiposPago: List<TipoPago>,
        recargo: Double?
    ): Curso {
        val curso = getById(cursoId)
        curso.horarios = horarios.toMutableList()
        curso.tiposPago = tiposPago.toMutableSet()
        curso.recargoAtraso = recargo?.let { BigDecimal.valueOf(it).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE) } ?: BigDecimal.ONE
        curso.estadoAlta = EstadoType.ACTIVO
        return save(curso)
    }
}