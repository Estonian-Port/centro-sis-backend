package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoAlquilerProfeRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.dto.response.PageResponse
import com.estonianport.centro_sis.dto.response.TipoPagoDto
import com.estonianport.centro_sis.mapper.CursoMapper.toDetalleDto
import com.estonianport.centro_sis.mapper.HorarioMapper
import com.estonianport.centro_sis.mapper.TipoPagoMapper
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.service.CursoService
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
class CursoController(private val cursoService: CursoService) {

    // ─── DEPRECADOS ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> =
        ok("Curso obtenido correctamente", cursoService.getByIdDto(id))

    @GetMapping("/inscripciones/{id}")
    fun getCursoConInscripciones(@PathVariable id: Long): ResponseEntity<CustomResponse> =
        ok("Curso obtenido correctamente", cursoService.getByIdDto(id))

    @GetMapping("/activos")
    fun getAllActivos(): ResponseEntity<CustomResponse> =
        ok("Cursos obtenidos correctamente", cursoService.getAllCursosResponse())

    @GetMapping("/activos-paginado")
    fun getAllActivosPaginado(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) estadoAlta: EstadoType?,
        @RequestParam(required = false) estadoCurso: String?
    ): ResponseEntity<PageResponse<*>> =
        ResponseEntity.ok(cursoService.getAllCursosPaginado(page, size, search, estadoAlta, estadoCurso))

    // ─── Lectura ──────────────────────────────────────────────────────────────

    /** Detalle de un curso, sin la lista de alumnos embebida. */
    @GetMapping("/{id}/detalle")
    fun getDetalle(@PathVariable id: Long): ResponseEntity<CustomResponse> =
        ok("Curso obtenido correctamente", cursoService.getDetalleDto(id))

    /** Listado liviano paginado para la pantalla de administración. */
    @GetMapping("/resumen")
    fun getResumenPaginado(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) estadoAlta: EstadoType?,
        @RequestParam(required = false) estadoCurso: String?
    ): ResponseEntity<PageResponse<*>> =
         ResponseEntity.ok(cursoService.getResumenPaginado(page, size, search, estadoAlta, estadoCurso))

    /** Listado liviano completo (sin paginar), para la vista de calendario. */
    @GetMapping("/resumen/activos")
    fun getActivosResumen(): ResponseEntity<CustomResponse> =
        ok("Cursos obtenidos correctamente", cursoService.getActivosResumen())

    /** Sub-recurso paginado de alumnos inscriptos en un curso. */
    @GetMapping("/{cursoId}/alumnos")
    fun getAlumnosInscriptos(
        @PathVariable cursoId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<*>> =
        ResponseEntity.ok(cursoService.getAlumnosInscriptosPaginado(cursoId, page, size))

    /** Vista "mi curso" desde la perspectiva de un alumno puntual. */
    @GetMapping("/{cursoId}/mi-inscripcion/{alumnoId}")
    fun getMiInscripcion(
        @PathVariable cursoId: Long,
        @PathVariable alumnoId: Long
    ): ResponseEntity<CustomResponse> =
        ok("Inscripción obtenida correctamente", cursoService.getMiInscripcion(cursoId, alumnoId))

    /** Vista "mi curso" desde la perspectiva de un alumno puntual. */
    @GetMapping("/{cursoId}/mis-pagos/{alumnoId}")
    fun getMisPagos(
        @PathVariable cursoId: Long,
        @PathVariable alumnoId: Long
    ): ResponseEntity<CustomResponse> =
        ok("Pagos obtenidos correctamente", cursoService.getMisPagos(cursoId, alumnoId))

    /** Cursos de un profesor, en formato liviano. */
    @GetMapping("/resumen/profesor/{idProfe}")
    fun getCursosResumenPorProfesorId(@PathVariable idProfe: Long): ResponseEntity<CustomResponse> =
        ok("Cursos obtenidos correctamente", cursoService.getCursosResumenPorProfesorId(idProfe))

    @GetMapping("/{cursoId}/partes-asistencia")
    fun getPartesAsistenciaCurso(@PathVariable cursoId: Long): ResponseEntity<CustomResponse> =
        ok("Partes de asistencia obtenidos correctamente", cursoService.getPartesAsistencia(cursoId))

    // ─── Alta ─────────────────────────────────────────────────────────────────

    @PostMapping("/alta-alquiler")
    fun altaAlquiler(@RequestBody cursoRequestDto: CursoAlquilerAdminRequestDto): ResponseEntity<CustomResponse> {
        val curso = cursoService.crearCursoAlquiler(cursoRequestDto)
        return ResponseEntity.status(201).body(
            CustomResponse(message = "Curso creado correctamente", data = curso.toDetalleDto(0))
        )
    }

    @PostMapping("/alta-comision")
    fun altaComision(@RequestBody cursoRequestDto: CursoComisionRequestDto): ResponseEntity<CustomResponse> {
        val curso = cursoService.crearCursoComision(cursoRequestDto)
        return ResponseEntity.status(201).body(
            CustomResponse(message = "Curso creado correctamente", data = curso.toDetalleDto(0))
        )
    }

    @PostMapping("/{cursoId}/finalizar-alta-alquiler")
    fun finalizarAltaCursoAlquiler(
        @PathVariable cursoId: Long,
        @RequestBody curso: CursoAlquilerProfeRequestDto
    ): ResponseEntity<CustomResponse> {
        val tiposDePago = curso.tiposPago.map { TipoPagoMapper.buildTipoPago(it) }.toSet()
        val cursoFinalizado = cursoService.finalizarAltaCursoAlquiler(cursoId, tiposDePago, curso.recargo)
        return ok(
            "Curso de alquiler finalizado correctamente",
            cursoFinalizado.toDetalleDto(cursoFinalizado.getInscripcionesActivas().size)
        )
    }

    // ─── Actualización ────────────────────────────────────────────────────────

    @PostMapping("/{cursoId}/actualizar-profesores")
    fun actualizarProfesoresDeCurso(
        @PathVariable cursoId: Long,
        @RequestBody profesoresId: List<Long>
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.reemplazarProfesoresPorId(cursoId, profesoresId)
        return ok(
            "Profesores actualizados correctamente",
            curso.toDetalleDto(curso.getInscripcionesActivas().size)
        )
    }

    @PutMapping("/{cursoId}/nombre")
    fun actualizarNombreCurso(
        @PathVariable cursoId: Long,
        @RequestParam nuevoNombre: String
    ): ResponseEntity<CustomResponse> =
        ok("Nombre del curso actualizado correctamente",
            cursoService.actualizarNombreCurso(cursoId, nuevoNombre))

    @PutMapping("/{cursoId}/profesores")
    fun actualizarProfesoresCurso(
        @PathVariable cursoId: Long,
        @RequestBody idsProfesores: List<Long>
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.reemplazarProfesoresPorId(cursoId, idsProfesores)
        return ok(
            "Profesores del curso actualizados correctamente",
            curso.toDetalleDto(curso.getInscripcionesActivas().size)
        )
    }

    @PutMapping("/{cursoId}/horarios")
    fun actualizarHorariosCurso(
        @PathVariable cursoId: Long,
        @RequestBody nuevosHorarios: List<HorarioDto>
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.actualizarHorariosCurso(
            cursoId, nuevosHorarios.map { HorarioMapper.buildHorario(it) }.toSet())
        return ok(
            "Horarios del curso actualizados correctamente",
            curso.toDetalleDto(curso.getInscripcionesActivas().size)
        )
    }

    @PutMapping("/{cursoId}/modalidades-pago")
    fun actualizarMontosTiposPagoCurso(
        @PathVariable cursoId: Long,
        @RequestBody montosActualizados: List<TipoPagoDto>
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.actualizarMontosTiposPagoCurso(
            cursoId, montosActualizados.map { TipoPagoMapper.buildTipoPago(it) }.toSet())
        return ok(
            "Montos de las modalidades de pago actualizados correctamente",
            curso.toDetalleDto(curso.getInscripcionesActivas().size)
        )
    }

    // ─── Asistencia ───────────────────────────────────────────────────────────

    @PostMapping("/{cursoId}/tomar-asistencia-automatica/{usuarioId}")
    fun tomarAsistenciaAutomatica(
        @PathVariable cursoId: Long,
        @PathVariable usuarioId: Long,
        @RequestParam(required = false) fecha: LocalDate?
    ): ResponseEntity<CustomResponse> {
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            return ResponseEntity.status(400).body(
                CustomResponse(message = "La fecha no puede ser futura", data = null))
        }
        cursoService.tomarAsistenciaAutomatica(cursoId, usuarioId, fecha)
        return ok("Asistencia tomada correctamente", null)
    }

    // ─── Baja ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/baja/{id}")
    fun baja(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        cursoService.delete(id)
        return ok("Curso dado de baja correctamente", null)
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun ok(message: String, data: Any?) =
        ResponseEntity.ok(CustomResponse(message = message, data = data))
}