package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.AccesoDTO
import com.estonianport.centro_sis.dto.EstadisticasAccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoDTO
import com.estonianport.centro_sis.dto.RegistrarAccesoQRRequest
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

    // =============================================
    // POST - Registrar acceso mediante QR
    // =============================================

    /**
     * Registrar acceso escaneando QR
     * Puede hacerlo: PORTERIA, ADMINISTRADOR, OFICINA
     *
     * Body: {
     *   "usuarioId": 123,
     *   "timestamp": 1234567890,  // Opcional (null para QR permanente)
     *   "tipo": "TEMPORAL" o "PERMANENTE"
     * }
     */
    @PostMapping("/registrar-qr/{registradoPorId}")
    fun registrarAccesoQR(
        @PathVariable registradoPorId: Long,
        @RequestBody request: RegistrarAccesoQRRequest
    ): ResponseEntity<AccesoDTO> {
        val acceso = accesoService.registrarAccesoQR(
            usuarioId = request.usuarioId,
            registradoPorId = registradoPorId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(acceso)
    }

    // =============================================
    // GET - Obtener accesos recientes
    // =============================================

    /**
     * Obtener últimos N accesos registrados
     * Puede hacerlo: PORTERIA, ADMINISTRADOR, OFICINA
     *
     * Default: últimos 50
     */
    @GetMapping("/recientes")
    fun getAccesosRecientes(
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<AccesoDTO>> {
        val accesos = accesoService.getAccesosRecientes(limit)
        return ResponseEntity.ok(accesos)
    }

    // =============================================
    // GET - Obtener accesos de un usuario
    // =============================================

    /**
     * Obtener accesos de un usuario específico
     * Puede hacerlo: Cualquier usuario (su propio historial)
     *
     * Default: últimos 30 días
     */
    @GetMapping("/usuario/{usuarioId}")
    fun getAccesosPorUsuario(
        @PathVariable usuarioId: Long,
        @RequestParam(defaultValue = "30") dias: Int
    ): ResponseEntity<List<AccesoDTO>> {
        val accesos = accesoService.getAccesosPorUsuario(usuarioId, dias)
        return ResponseEntity.ok(accesos)
    }

    // =============================================
    // GET - Estadísticas de accesos (OPCIONAL)
    // =============================================

    /**
     * Obtener estadísticas de accesos
     * Puede hacerlo: ADMINISTRADOR, OFICINA
     *
     * Ejemplo: total de accesos hoy, esta semana, este mes
     */
    @GetMapping("/estadisticas")
    fun getEstadisticasAccesos(): ResponseEntity<EstadisticasAccesoDTO> {
        val estadisticas = accesoService.getEstadisticasAccesos()
        return ResponseEntity.ok(estadisticas)
    }


}