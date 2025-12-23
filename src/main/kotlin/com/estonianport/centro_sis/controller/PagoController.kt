package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.PagoMapper
import com.estonianport.centro_sis.model.CursoAlquiler
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.InscripcionService
import com.estonianport.centro_sis.service.PagoService
import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pago")
@CrossOrigin("*")
class PagoController(
    private val pagoService: PagoService,
    private val inscripcionService: InscripcionService,
    private val usuarioService: UsuarioService,
    private val cursoService: CursoService
) {

    //Obtener el detalle de un pago por id
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val pago = pagoService.get(id)!!

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pago obtenido correctamente",
                data = PagoMapper.buildPagoResponseDto(pago)
            )
        )
    }

    //Registrar pago de alumno a instituto o profesor
    @PutMapping("/registrarPagoAlumno/{inscripcionId}/{usuarioId}")
    fun registrarPagoAlumno(
        @PathVariable inscripcionId: Long,
        @PathVariable usuarioId: Long
    ): ResponseEntity<CustomResponse> {
        val inscripcion = inscripcionService.getById(inscripcionId)
        val usuario = usuarioService.getById(usuarioId)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pago registrado correctamente",
                data = pagoService.registrarPagoAlumno(inscripcion, usuario)
            )
        )
    }

    //Registrar pago de profesor a instituto
    @PutMapping("/registrarPagoProfesor/{usuarioId}/{cursoId}")
    fun registrarPagoProfesor(
        @PathVariable usuarioId: Long,
        @PathVariable cursoId: Long
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(cursoId)
        val usuario = usuarioService.getById(usuarioId)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pago registrado correctamente",
                data = pagoService.registrarPagoAlquilerProfesor(curso as CursoAlquiler, usuario)
            )
        )
    }

    //Registrar pago de instituto a profesor
    @PutMapping("/registrarPagoInstituto/{usuarioId}/{cursoId}/{profesorId}")
    fun registrarPagoInstituto(
        @PathVariable usuarioId: Long,
        @PathVariable cursoId: Long,
        @PathVariable profesorId: Long
    ): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(cursoId)
        val usuario = usuarioService.getById(usuarioId)
        val profesor = usuarioService.getById(profesorId).getRolProfesor()
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pago registrado correctamente",
                data = pagoService.registrarComisionProfesor(curso as CursoComision, profesor, usuario)
            )
        )
    }
}