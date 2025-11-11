package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.PagoMapper
import com.estonianport.centro_sis.service.PagoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pago")
@CrossOrigin("*")
class PagoController(private val pagoService: PagoService) {

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
    @GetMapping("/getAllByUsuarioId/{id}")
    fun getAllByUsuarioId(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val listaPago = pagoService.getAllByUsuarioId(id)

        val listaPagoDto = listaPago.map{ PagoMapper.buildPagoResponseDto(it) }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pago obtenido correctamente",
                data = listaPagoDto
            )
        )
    }
}