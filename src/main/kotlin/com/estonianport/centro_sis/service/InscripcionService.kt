package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.request.InscripcionRequestDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.PagoCurso
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.repository.InscripcionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InscripcionService() {

    @Autowired
    lateinit var inscripcionRepository: InscripcionRepository

    fun getById(id: Long): Inscripcion {
        return inscripcionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Inscripción con id $id no encontrada") }
    }

    fun inscribirUsuarioACurso(usuario: Usuario, curso: Curso, inscripcion: InscripcionRequestDto): Inscripcion {
        val alumno = usuario.getRolAlumno()
        // Verificar que el curso acepte ese tipo de pago
        val tipoPagoDisponible =
            curso.tiposPago.firstOrNull { it.tipo == PagoType.valueOf(inscripcion.tipoPagoSeleccionado) }
                ?: throw IllegalArgumentException("El curso ${curso.nombre} no acepta el tipo de pago $inscripcion.tipoPagoSeleccionado")

        val inscripcion = Inscripcion(
            alumno = alumno,
            curso = curso,
            tipoPagoSeleccionado = tipoPagoDisponible,
            beneficio = inscripcion.beneficio
        )
        alumno.inscribirEnCurso(inscripcion)
        return inscripcionRepository.save(inscripcion)
    }

    fun editarBeneficio(idInscripcion: Long, idUsuario: Long, nuevoBeneficio: Int): Inscripcion {
        val inscripcion = getById(idInscripcion)
        // Verificar que el usuario tenga permiso para asignar beneficios
        inscripcion.verificarPermisoEdicion(inscripcion.alumno.usuario)
        // Actualizar el beneficio
        inscripcion.aplicarBeneficio(nuevoBeneficio)
        return inscripcionRepository.save(inscripcion)
    }

    fun darDeBajaInscripcion(idInscripcion: Long) {
        val inscripcion = getById(idInscripcion)
        inscripcion.darDeBaja()
        inscripcionRepository.save(inscripcion)
    }

    fun obtenerPagosAlumno(alumnoId: Long, inscripcionId: Long): List<PagoCurso> {
        val inscripcion = getById(inscripcionId)
        if (inscripcion.alumno.id != alumnoId) {
            throw IllegalArgumentException("El alumno con id $alumnoId no está inscrito en la inscripción con id $inscripcionId")
        }
        return inscripcion.pagos
    }

    fun obtenerTodosLosPagosAlumno(alumnoId: Long): List<PagoCurso> {
        val inscripciones = inscripcionRepository.findAll().filter { it.alumno.id == alumnoId }
        return inscripciones.flatMap { it.pagos }
    }

}