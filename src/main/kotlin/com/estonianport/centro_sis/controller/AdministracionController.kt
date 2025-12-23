package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.service.AdministracionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
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

    /*
    // MovimientoFinancieroController o AdministracionController
    @GetMapping("/pagos")
    fun obtenerTodosLosPagos(
        @RequestParam(required = false) tipo: TipoPago?,
        @RequestParam(required = false) desde: LocalDate?,
        @RequestParam(required = false) hasta: LocalDate?
    ): ResponseEntity<List<PagoDTO>> {

        // Opción 1: Usar MovimientoFinanciero (vista consolidada)
        val movimientos = movimientoRepository.findAll()

        // Opción 2: Consultar todos los tipos de pago por separado
        val pagosInscripcion = pagoInscripcionRepository.findAll()
        val pagosAlquiler = pagoAlquilerRepository.findAll()
        val pagosComision = pagoComisionRepository.findAll()

        // Consolidar en un solo DTO
        val todosPagos = buildList {
            addAll(pagosInscripcion.map { PagoMapper.toDto(it) })
            addAll(pagosAlquiler.map { PagoMapper.toDto(it) })
            addAll(pagosComision.map { PagoMapper.toDto(it) })
        }

        return ResponseEntity.ok(todosPagos)
    }
    */

}