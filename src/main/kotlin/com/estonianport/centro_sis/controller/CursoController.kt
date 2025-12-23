package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.request.CursoRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.service.CursoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/curso")
@CrossOrigin("*")
class CursoController(
    private val cursoService: CursoService,
) {

    //Obtener un curso por id
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(id)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(curso)
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
                data = cursos.map { CursoMapper.buildCursoResponseDto(it) }
            )
        )
    }

    /*
        REVISAR ESTOS TRES ENDPOINTS DE ABAJO

        //Crear un nuevo curso
        @PostMapping("/alta")
        fun alta(@RequestBody cursoRequestDto: CursoRequestDto): ResponseEntity<CustomResponse> {
            val nuevoCurso = CursoMapper.buildCurso(cursoRequestDto)
            val cursoAgregado = cursoService.alta(nuevoCurso)

            //Una vez que el curso se creo correctamente, osea no ya esta persistido,
            //le asigno rol de profesor a los usuarios seleccionados
            cursoRequestDto.profesoresId.forEach { usuarioId ->
                val usuario = usuarioService.getById(usuarioId)
                val rol = rolFactory.build("PROFESOR", usuario)
                usuario.asignarRol(rol)
                usuarioService.save(usuario)
            }

            return ResponseEntity.status(201).body(
                CustomResponse(
                    message = "Curso creado correctamente",
                    data = cursoAgregado
                )
            )
        }

        //Dar de baja un curso
        @PostMapping("/baja/{id}")
        fun baja(@PathVariable id: Long): ResponseEntity<CustomResponse> {
            cursoService.delete(id)
            return ResponseEntity.status(200).body(
                CustomResponse(
                    message = "Curso dado de baja correctamente",
                    data = null
                )
            )
        }

        //Editar un curso
        @PostMapping("/editar/{id}")
        fun editar(@PathVariable id: Long, @RequestBody cursoRequestDto: CursoRequestDto): ResponseEntity<CustomResponse> {
            val cursoExistente = cursoService.getById(id)
            val cursoEditado = CursoMapper.buildCurso(cursoRequestDto, cursoExistente)
            cursoService.save(cursoEditado)
            return ResponseEntity.status(200).body(
                CustomResponse(
                    message = "Curso editado correctamente",
                    data = cursoEditado
                )
            )
        }

     */
}