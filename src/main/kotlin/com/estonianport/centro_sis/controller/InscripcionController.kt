package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.request.InscripcionRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.InscripcionMapper
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.InscripcionService
import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inscripcion")
@CrossOrigin("*")
class InscripcionController(
    private val inscripcionService: InscripcionService,
    private val usuarioService: UsuarioService,
    private val cursoService: CursoService,
) {

    // Inscribir un usuario a un curso
    @PostMapping("/usuario/{idUsuario}/curso/{idCurso}")
    fun inscribirUsuarioACurso(
        @PathVariable idUsuario: Long,
        @PathVariable idCurso: Long,
        @RequestBody inscripcionDto: InscripcionRequestDto
    ): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(idUsuario)
        val curso = cursoService.getById(idCurso)
        val inscripcion = inscripcionService.inscribirUsuarioACurso(usuario, curso, inscripcionDto)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Inscripción realizada correctamente",
                data = inscripcion
            )
        )
    }

    //Obtener el detalle de una inscripcion por id
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val inscripcion = inscripcionService.getById(id)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Inscripción obtenida correctamente",
                data = InscripcionMapper.buildInscripcionResponseDto(inscripcion)
            )
        )
    }

    //Aplicar/modificar un beneficio a una inscripcion
    @PutMapping("/editar-beneficio/{idInscripcion}/{idUsuario}")
    fun aplicarBeneficio(
        @PathVariable idInscripcion: Long,
        @PathVariable idUsuario: Long,
        @RequestBody beneficio: Int
    ): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Beneficio aplicado/modificado correctamente",
                data = inscripcionService.editarBeneficio(idInscripcion, idUsuario, beneficio)
            )
        )
    }

    //Dar de baja una inscripcion
    @DeleteMapping("/baja/{idInscripcion}")
    fun darDeBajaInscripcion(
        @PathVariable idInscripcion: Long
    ): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Inscripción dada de baja correctamente",
                data = inscripcionService.darDeBajaInscripcion(idInscripcion)
            )
        )
    }

}