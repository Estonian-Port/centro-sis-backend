package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.service.AdministracionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/administracion")
@CrossOrigin("*")
class AdministracionController(
    private val administracionService: AdministracionService,
) {

    @GetMapping("/estadisticas")
    fun getEstadisticas(): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Estadísticas obtenidas correctamente",
                data = administracionService.getEstadisticas()
            )
        )
    }

    //Endpoint para registrar un acceso manualmente
    @PostMapping("/acceso/manual/{idAdmin}/{idUsuario}")
    fun registrarAccesoManual(
        @PathVariable idAdmin: Long,
        @PathVariable idUsuario: Long
    ): ResponseEntity<CustomResponse> {
        administracionService.registrarAccesoManual(idAdmin, idUsuario)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Acceso registrado manualmente",
                data = null
            )
        )
    }

}