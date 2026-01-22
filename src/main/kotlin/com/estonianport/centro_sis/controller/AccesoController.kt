package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.AccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoDTO
import com.estonianport.centro_sis.dto.response.PageResponse
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.service.AccesoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/accesos")
@CrossOrigin(origins = ["*"])
class AccesoController(
    private val accesoService: AccesoService
) {

    // ========================================
    // GET - MIS ACCESOS
    // ========================================

    /**
     * Obtener mis accesos (usuario actual)
     */
    @GetMapping("/mis-accesos/{idUsuario}")
    fun getMisAccesos(
        @PathVariable idUsuario: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<AccesoDTO>> {
        val accesosPage = accesoService.getMisAccesos(
            usuarioId = idUsuario,
            page = page,
            size = size,
            meses = meses
        )

        // Convertir Page a PageResponse para evitar warning
        return ResponseEntity.ok(PageResponse.from(accesosPage))
    }

    // ========================================
    // GET - TODOS LOS ACCESOS
    // ========================================

    /**
     * Obtener todos los accesos con filtros (Admin/Oficina)
     */
    @GetMapping("/todos/{idUsuario}")
    fun getTodosAccesos(
        @PathVariable idUsuario: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) roles: List<RolType>?,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<AccesoDTO>> {
        val accesosPage = accesoService.getTodosAccesos(
            usuarioId = idUsuario,
            page = page,
            size = size,
            search = search,
            roles = roles,
            meses = meses
        )

        // Convertir Page a PageResponse para evitar warning
        return ResponseEntity.ok(PageResponse.from(accesosPage))
    }

    // ========================================
    // POST - REGISTRAR ACCESO MANUAL
    // ========================================

    /**
     * Registrar acceso manualmente (Admin/Oficina)
     */
    @PostMapping("/manual/{idAdmin}")
    fun registrarAccesoManual(
        @PathVariable idAdmin: Long,
        @RequestBody dto: RegistrarAccesoDTO
    ): ResponseEntity<AccesoDTO> {
        val acceso = accesoService.registrarAccesoManual(idAdmin, dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(acceso)
    }

    // ========================================
    // POST - REGISTRAR ACCESO QR (futuro)
    // ========================================

    /**
     * Registrar acceso por QR
     */
    @PostMapping("/qr/{idUsuario}")
    fun registrarAccesoQR(
        @PathVariable idUsuario: Long
    ): ResponseEntity<AccesoDTO> {
        val acceso = accesoService.registrarAccesoQR(idUsuario)
        return ResponseEntity.status(HttpStatus.CREATED).body(acceso)
    }
}