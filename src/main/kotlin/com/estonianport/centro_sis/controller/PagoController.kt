package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.*
import com.estonianport.centro_sis.dto.response.PageResponse
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoPagoConcepto
import com.estonianport.centro_sis.service.PagoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pagos")
@CrossOrigin(origins = ["*"])
class PagoController(
    private val pagoService: PagoService
) {

    // ========================================
    // GET - PAGOS RECIBIDOS
    // ========================================

    /**
     * Obtener pagos recibidos paginados
     *
     * - Admin/Oficina: CURSO (alumnos → instituto) + ALQUILER (profesores → instituto)
     * - Profesor: CURSO (alumnos → profesor en sus cursos alquiler) + COMISION (instituto → profesor)
     */
    @GetMapping("/recibidos/{idUsuario}")
    fun getPagosRecibidos(
        @PathVariable idUsuario: Long,
        @RequestParam rolActivo: RolType, // ✅ NUEVO: desde frontend
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) tipos: List<TipoPagoConcepto>?,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<PagoDTO>> {
        val pagosPage = pagoService.getPagosRecibidos(
            usuarioId = idUsuario,
            rolActivo = rolActivo, // ✅ NUEVO
            page = page,
            size = size,
            search = search,
            tipos = tipos,
            meses = meses
        )

        return ResponseEntity.ok(PageResponse.from(pagosPage))
    }

    // ========================================
    // GET - PAGOS REALIZADOS
    // ========================================

    /**
     * Obtener pagos realizados paginados
     *
     * - Admin/Oficina: COMISION (instituto → profesores)
     * - Profesor: ALQUILER (profesor → instituto)
     * - Alumno: CURSO (alumno → instituto/profesor)
     */
    @GetMapping("/realizados/{idUsuario}")
    fun getPagosRealizados(
        @PathVariable idUsuario: Long,
        @RequestParam rolActivo: RolType, // ✅ NUEVO: desde frontend
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<PagoDTO>> {
        val pagosPage = pagoService.getPagosRealizados(
            usuarioId = idUsuario,
            rolActivo = rolActivo, // ✅ NUEVO
            page = page,
            size = size,
            search = search,
            meses = meses
        )

        return ResponseEntity.ok(PageResponse.from(pagosPage))
    }

    // ========================================
    // POST - GENERAR PREVIEW PAGO
    // ========================================


    @PostMapping("/curso/preview/{idUsuario}")
    fun calcularPreviewPago(
        @PathVariable idUsuario: Long,
        @RequestBody request: PagoPreviewRequest
    ): ResponseEntity<PagoPreviewDTO> {
        val preview = pagoService.calcularPreviewPago(
            usuarioId = idUsuario,
            inscripcionId = request.inscripcionId,
            aplicarRecargo = request.aplicarRecargo
        )
        return ResponseEntity.ok(preview)
    }

    @PostMapping("/alquiler/preview/{idUsuario}/{profesorId}")
    fun calcularPreviewAlquiler(
        @PathVariable idUsuario: Long,
        @PathVariable profesorId: Long,
        @RequestBody request: PagoAlquilerPreviewRequest
    ): ResponseEntity<PagoAlquilerPreviewDTO> {
        val preview = pagoService.calcularPreviewAlquiler(
            usuarioId = idUsuario,
            cursoId = request.cursoId,
            profesorId = profesorId
        )
        return ResponseEntity.ok(preview)
    }

    @PostMapping("/comision/preview/{idUsuario}")
    fun calcularPreviewComision(
        @PathVariable idUsuario: Long,
        @RequestBody request: PagoComisionPreviewRequest
    ): ResponseEntity<PagoComisionPreviewDTO> {
        val preview = pagoService.calcularPreviewComision(
            usuarioId = idUsuario,
            cursoId = request.cursoId,
            profesorId = request.profesorId
        )
        return ResponseEntity.ok(preview)
    }

    // ========================================
    // POST - REGISTRAR PAGOS
    // ========================================

    /**
     * Registrar pago de curso (alumno → instituto/profesor)
     *
     * Puede hacerlo: Admin, Oficina, Profesor del curso
     */
    @PostMapping("/curso/{idUsuario}")
    fun registrarPagoCurso(
        @PathVariable idUsuario: Long,
        @RequestBody request: RegistrarPagoCursoRequest
    ): ResponseEntity<PagoDTO> {
        val pago = pagoService.registrarPagoCurso(
            usuarioId = idUsuario,
            inscripcionId = request.inscripcionId,
            aplicarRecargo = request.aplicarRecargo
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(pago)
    }

    @PostMapping("/alquiler/{idUsuario}")
    fun registrarPagoAlquiler(
        @PathVariable idUsuario: Long,
        @RequestBody request: RegistrarPagoAlquilerRequest
    ): ResponseEntity<PagoDTO> {
        val pago = pagoService.registrarPagoAlquiler(
            usuarioId = idUsuario,
            cursoId = request.cursoId,
            numeroCuota = request.numeroCuota
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(pago)
    }

    @PostMapping("/comision/{idUsuario}")
    fun registrarPagoComision(
        @PathVariable idUsuario: Long,
        @RequestBody request: RegistrarPagoComisionRequest
    ): ResponseEntity<PagoDTO> {
        val pago = pagoService.registrarPagoComision(
            usuarioId = idUsuario,
            cursoId = request.cursoId,
            profesorId = request.profesorId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(pago)
    }


    // ========================================
    // POST - ANULAR PAGO
    // ========================================

    /**
     * Anular un pago (solo Admin)
     */
    @PostMapping("/{pagoId}/anular/{idUsuario}")
    fun anularPago(
        @PathVariable pagoId: Long,
        @PathVariable idUsuario: Long,
        @RequestBody dto: AnularPagoDTO
    ): ResponseEntity<Void> {
        pagoService.anularPago(idUsuario, pagoId, dto)
        return ResponseEntity.noContent().build()
    }

    /**
     * Obtener pagos de un curso específico
     *
     * - Si es ALQUILER: Pagos de alquiler del curso
     * - Si es COMISION: Pagos de comisión del curso
     */
    @GetMapping("/curso/{cursoId}")
    fun getPagosPorCurso(
        @PathVariable cursoId: Long
    ): ResponseEntity<List<PagoDTO>> {
        val pagos = pagoService.getPagosPorCurso(cursoId)
        return ResponseEntity.ok(pagos)
    }
}