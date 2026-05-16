package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.dto.request.InscripcionRequestDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.PagoCurso
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.TipoPago
import com.estonianport.centro_sis.repository.InscripcionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InscripcionService {

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @Autowired
    lateinit var inscripcionRepository: InscripcionRepository

    // Modificado para usar la query optimizada con FETCH
    @Transactional(readOnly = true)
    fun getById(id: Long): Inscripcion {
        return inscripcionRepository.findByIdWithCursoAndAlumno(id).getOrNull()
            ?: throw NoSuchElementException("Inscripción con id $id no encontrada")
    }

    @Transactional
    fun inscribirUsuarioACurso(usuario: Usuario, curso: Curso, inscripcion: InscripcionRequestDto): Inscripcion {
        val alumno = usuario.getRolAlumno()

        val tipoPagoDisponible =
            curso.tiposPago.firstOrNull { it.tipo == PagoType.valueOf(inscripcion.tipoPagoSeleccionado) }
                ?: throw IllegalArgumentException("El curso ${curso.nombre} no acepta el tipo de pago ${inscripcion.tipoPagoSeleccionado}")

        val tipoPagoAjustado = when (tipoPagoDisponible.tipo) {
            PagoType.TOTAL -> TipoPago(
                tipo = tipoPagoDisponible.tipo,
                monto = tipoPagoDisponible.monto,
                cuotas = 1
            )
            PagoType.MENSUAL -> tipoPagoDisponible
        }

        val nuevaInscripcion = Inscripcion(
            alumno = alumno,
            curso = curso,
            tipoPagoSeleccionado = tipoPagoAjustado,
            beneficio = inscripcion.beneficio,
        )

        alumno.inscribirEnCurso(nuevaInscripcion)
        return inscripcionRepository.save(nuevaInscripcion)
    }

    @Transactional
    fun asignarPuntos(idInscripcion: Long, puntos: Int, usuario: Usuario): Inscripcion {
        val inscripcion = getById(idInscripcion)
        inscripcion.darPuntos(usuario, puntos)
        return inscripcionRepository.save(inscripcion)
    }

    @Transactional
    fun editarBeneficio(idInscripcion: Long, nuevoBeneficio: Int, idUsuario: Long): Inscripcion {
        val inscripcion = getById(idInscripcion)
        val otorgadoPor = usuarioService.getById(idUsuario)
        inscripcion.verificarPermisoEdicion(otorgadoPor)
        inscripcion.aplicarBeneficio(nuevoBeneficio)
        return inscripcionRepository.save(inscripcion)
    }

    @Transactional
    fun darDeBajaInscripcion(idInscripcion: Long) {
        val inscripcion = getById(idInscripcion)
        inscripcion.darDeBaja()
        inscripcionRepository.save(inscripcion)
    }

    @Transactional(readOnly = true)
    fun obtenerPagosAlumno(alumnoId: Long, inscripcionId: Long): List<PagoCurso> {
        val inscripcion = getById(inscripcionId)
        if (inscripcion.alumno.id != alumnoId) {
            throw IllegalArgumentException("El alumno con id $alumnoId no está inscrito en la inscripción con id $inscripcionId")
        }
        return inscripcion.pagos
    }

    // Solución radical al problema N+1 y al desborde de memoria
    @Transactional(readOnly = true)
    fun obtenerTodosLosPagosAlumno(alumnoId: Long): List<PagoCurso> {
        return inscripcionRepository.findAllPagosByAlumnoId(alumnoId)
    }
}