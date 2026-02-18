package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoAlquilerProfeRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.dto.response.TipoPagoDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.HorarioMapper
import com.estonianport.centro_sis.mapper.ParteAsistenciaMapper
import com.estonianport.centro_sis.mapper.TipoPagoMapper
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/curso")
@CrossOrigin("*")
class CursoController(
    private val cursoService: CursoService,
    private val usuarioService: UsuarioService,
) {

    //Obtener un curso por id
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(id)
        val alumnosInscriptos = curso.inscripciones
            .filter { it.estado == EstadoType.ACTIVO }
            .map { UsuarioMapper.buildAlumno(it.alumno.usuario) }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    curso, alumnosInscriptos
                )
            )
        )
    }

    //Obtetener un curso por id con sus inscripciones
    @GetMapping("/inscripciones/{id}")
    fun getCursoConInscripciones(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(id)
        val alumnosInscriptos = curso.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    curso, alumnosInscriptos
                )
            )
        )
    }

    //Obtener todos los cursos activos
    @GetMapping("/activos")
    fun get(): ResponseEntity<CustomResponse> {
        val cursos = cursoService.getAllCursos()

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = cursos.map { curso ->
                    CursoMapper.buildCursoResponseDto(
                        curso,
                        curso.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) }
                    )
                }
            )
        )
    }

    //Crear un nuevo curso alquiler
    @PostMapping("/alta-alquiler")
    fun altaAlquiler(@RequestBody cursoRequestDto: CursoAlquilerAdminRequestDto): ResponseEntity<CustomResponse> {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoAlquiler = CursoMapper.buildCursoAlquiler(cursoRequestDto, profesores)
        val cursoAgregado = cursoService.alta(nuevoCursoAlquiler)

        return ResponseEntity.status(201).body(
            CustomResponse(
                message = "Curso creado correctamente",
                data = cursoAgregado
            )
        )
    }

    //Crear un nuevo curso comision
    @PostMapping("/alta-comision")
    fun altaComision(@RequestBody cursoRequestDto: CursoComisionRequestDto): ResponseEntity<CustomResponse> {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoComision = CursoMapper.buildCursoComision(cursoRequestDto, profesores)
        nuevoCursoComision.estadoAlta = EstadoType.ACTIVO

        val cursoAgregado = cursoService.alta(nuevoCursoComision)

        return ResponseEntity.status(201).body(
            CustomResponse(
                message = "Curso creado correctamente",
                data = cursoAgregado
            )
        )
    }

    // Actualizar profesores de un curso
    @PostMapping("/{cursoId}/actualizar-profesores")
    fun actualizarProfesoresDeCurso(
        @PathVariable cursoId: Long,
        @RequestBody profesoresId: List<Long>
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(cursoId)
        val nuevosProfesores = profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Profesores actualizados correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoService.actualizarProfesoresDelCurso(curso, nuevosProfesores),
                    curso.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para finalizar alta de un curso de alquiler
    @PostMapping("/{cursoId}/finalizar-alta-alquiler")
    fun finalizarAltaCursoAlquiler(
        @PathVariable cursoId: Long,
        @RequestBody curso: CursoAlquilerProfeRequestDto
    ): ResponseEntity<CustomResponse> {
        val horarios = curso.horarios.map {
            HorarioMapper.buildHorario(it)
        }
        val tiposDePago = curso.tiposPago.map {
            TipoPagoMapper.buildTipoPago(it)
        }

        val cursoFinalizado = cursoService.finalizarAltaCursoAlquiler(cursoId, horarios, tiposDePago, curso.recargo)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso de alquiler finalizado correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoFinalizado,
                    cursoFinalizado.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para actualizar el nombre de un curso
    @PutMapping("/{cursoId}/nombre")
    fun actualizarNombreCurso(
        @PathVariable cursoId: Long,
        @RequestParam nuevoNombre: String
    ): ResponseEntity<CustomResponse> {
        val cursoActualizado = cursoService.actualizarNombreCurso(cursoId, nuevoNombre)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Nombre del curso actualizado correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoActualizado,
                    cursoActualizado.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para actualizar los profesores de un curso
    @PutMapping("/{cursoId}/profesores")
    fun actualizarNombreCurso(
        @PathVariable cursoId: Long,
        @RequestBody idsProfesores: List<Long>
    ): ResponseEntity<CustomResponse> {
        val profesores = idsProfesores.map { usuarioService.getById(it).getRolProfesor() }
        val cursoActualizado = cursoService.actualizarProfesores(cursoId, profesores)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Nombre del curso actualizado correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoActualizado,
                    cursoActualizado.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para editar los horarios de un curso
    @PutMapping("/{cursoId}/horarios")
    fun actualizarHorariosCurso(
        @PathVariable cursoId: Long,
        @RequestBody nuevosHorarios: List<HorarioDto>
    ): ResponseEntity<CustomResponse> {
        val cursoActualizado = cursoService.actualizarHorariosCurso(
            cursoId,
            nuevosHorarios.map { HorarioMapper.buildHorario(it) }
        )
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Horarios del curso actualizados correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoActualizado,
                    cursoActualizado.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para editar montos de las modalidades de pago permitidas en un curso
    @PutMapping("/{cursoId}/modalidades-pago")
    fun actualizarMontosTiposPagoCurso(
        @PathVariable cursoId: Long,
        @RequestBody montosActualizados: List<TipoPagoDto>,
    ): ResponseEntity<CustomResponse> {
        val cursoActualizado = cursoService.actualizarMontosTiposPagoCurso(
            cursoId,
            montosActualizados.map { TipoPagoMapper.buildTipoPago(it) }
        )
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Montos de las modalidades de pago actualizados correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    cursoActualizado,
                    cursoActualizado.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) })
            )
        )
    }

    //Endpoint para obtener los partes de asistencia de un curso
    @GetMapping("/{cursoId}/partes-asistencia")
    fun getPartesAsistenciaCurso(
        @PathVariable cursoId: Long
    ): ResponseEntity<CustomResponse> {
        val partes = cursoService.getPartesAsistencia(cursoId)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Partes de asistencia obtenidos correctamente",
                data = partes.map {
                    ParteAsistenciaMapper.buildParteAsistenciaResponseDto(it)
                }
            )
        )
    }

    //Endpoint para tomar asistencia en un curso
    @PostMapping("/{cursoId}/tomar-asistencia-automatica/{usuarioId}")
    fun tomarAsistenciaAutomatica(
        @PathVariable cursoId: Long,
        @PathVariable usuarioId: Long,
        @RequestParam(required = false) fecha: LocalDate?
    ): ResponseEntity<CustomResponse> {
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            return ResponseEntity.status(400).body(
                CustomResponse(
                    message = "La fecha no puede ser futura",
                    data = null
                )
            )
        }
        cursoService.tomarAsistenciaAutomatica(cursoId, usuarioId, fecha)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Asistencia tomada correctamente",
                data = null
            )
        )
    }

    //Dar de baja un curso
    @DeleteMapping("/baja/{id}")
    fun baja(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        cursoService.delete(id)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso dado de baja correctamente",
                data = null
            )
        )
    }

}