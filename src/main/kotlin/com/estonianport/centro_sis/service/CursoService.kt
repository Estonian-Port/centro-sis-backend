package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Horario
import com.estonianport.centro_sis.model.ParteAsistencia
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.enums.TipoPago
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.repository.CursoRepository
import com.estonianport.centro_sis.repository.InscripcionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional // Import agregado
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CursoService : GenericServiceImpl<Curso, Long>() {

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @Autowired
    lateinit var cursoRepository: CursoRepository

    override val dao: CursoRepository
        get() = cursoRepository

    fun countCursos(): Long {
        return cursoRepository.countByFechaBajaIsNull()
    }

    fun getById(id: Long): Curso {
        return cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
    }

    fun getAllCursos(): List<Curso> {
        return cursoRepository.findAllOrdenados()
    }

    @Transactional
    fun alta(nuevoCurso: Curso): Curso {
        return cursoRepository.save(nuevoCurso)
    }

    @Transactional
    override fun delete(id: Long) {
        val curso: Curso = cursoRepository.findById(id).get()
        curso.darDeBaja()
        cursoRepository.save(curso)
    }

    @Transactional
    fun actualizarProfesoresDelCurso(curso: Curso, nuevosProfesores: List<RolProfesor>): Curso {
        curso.profesores.clear()
        curso.profesores.addAll(nuevosProfesores)
        return save(curso)
    }

    @Transactional
    fun finalizarAltaCursoAlquiler(
        cursoId: Long,
        tiposPago: List<TipoPago>,
        recargo: Double?
    ): Curso {
        val curso = getById(cursoId)
        curso.tiposPago = tiposPago.toMutableSet()
        curso.recargoAtraso =
            recargo?.let { BigDecimal.valueOf(it).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE) }
                ?: BigDecimal.ONE
        curso.estadoAlta = EstadoType.ACTIVO
        return save(curso)
    }

    @Transactional
    fun actualizarNombreCurso(cursoId: Long, nuevoNombre: String): Curso {
        val curso = getById(cursoId)
        curso.nombre = nuevoNombre
        return save(curso)
    }

    @Transactional
    fun actualizarProfesores(cursoId: Long, nuevosProfesores: List<RolProfesor>): Curso {
        val curso = getById(cursoId)
        //Agregar a los profesores que no estaban previamente asignados
        nuevosProfesores.forEach {
            if (!curso.esProfesor(it)) {
                curso.agregarProfesor(it)
            }
        }
        //Eliminar a los profesores que ya no están asignados
        curso.profesores.toList().forEach {
            if (!nuevosProfesores.contains(it)) {
                curso.removerProfesor(it)
            }
        }

        return save(curso)
    }

    @Transactional
    fun actualizarHorariosCurso(cursoId: Long, nuevosHorarios: List<Horario>): Curso {
        val curso = getById(cursoId)
        curso.horarios.clear()
        curso.horarios.addAll(nuevosHorarios)
        return save(curso)
    }

    fun getPartesAsistencia(cursoId: Long): List<ParteAsistencia> {
        return cursoRepository.findPartesAsistenciaByCursoId(cursoId)
    }

    @Transactional
    fun actualizarMontosTiposPagoCurso(cursoId: Long, nuevosTiposPago: List<TipoPago>): Curso {
        val curso = getById(cursoId)
        curso.tiposPago.clear()
        curso.tiposPago.addAll(nuevosTiposPago)
        return save(curso)
    }

    @Transactional
    fun tomarAsistenciaAutomatica(cursoId: Long, usuarioId: Long, fecha: LocalDate?): Curso {
        val curso = getById(cursoId)
        val usuario = usuarioService.getById(usuarioId)
        curso.tomarAsistencia(usuario, fecha ?: LocalDate.now())
        return save(curso)
    }

    fun obtenerCursosProfesorId(idProfe: Long): List<Curso> {
        return cursoRepository.findCursosActivosPorProfesorId(idProfe)
    }
}