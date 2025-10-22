package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.service.CursoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/curso")
@CrossOrigin("*")
class CursoController(private val cursoService: CursoService) {

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.get(id)!!

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(curso)
            )
        )
    }
    @GetMapping("getByUsuarioId/{id}")
    fun getByUsuarioId(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        //val curso = cursoService.getByUsuarioId(id)!!
        val curso = cursoService.get(id)!!

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(curso)
            )
        )
    }
}