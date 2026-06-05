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

    // PAGOS RECIBIDOS
    @GetMapping("/recibidos/{idUsuario}")
    fun getPagosRecibidos(
        @PathVariable idUsuario: Long,
        @RequestParam rolActivo: RolType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) tipos: List<TipoPagoConcepto>?,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<PagoDTO>> {
        val pagosPage = pagoService.getPagosRecibidos(
            usuarioId = idUsuario,
            rolActivo = rolActivo,
            page = page,
            size = size,
            search = search,
            tipos = tipos,
            meses = meses
        )

        return ResponseEntity.ok(PageResponse.from(pagosPage))
    }

    // PAGOS REALIZADOS
    @GetMapping("/realizados/{idUsuario}")
    fun getPagosRealizados(
        @PathVariable idUsuario: Long,
        @RequestParam rolActivo: RolType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) meses: List<Int>?
    ): ResponseEntity<PageResponse<PagoDTO>> {
        val pagosPage = pagoService.getPagosRealizados(
            usuarioId = idUsuario,
            rolActivo = rolActivo,
            page = page,
            size = size,
            search = search,
            meses = meses
        )

        return ResponseEntity.ok(PageResponse.from(pagosPage))
    }

    // GENERAR PREVIEW PAGO
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

    // REGISTRAR PAGOS: Registrar pago de curso (alumno → instituto/profesor)
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

    // Registrar pago de alquiler (profesor → instituto)
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

    // Registrar pago de comisión (instituto → profesor)
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
    // MATRÍCULA (pago anual del alumno al instituto)
    // ========================================

    // Preview de matrícula (monto + si ya pagó)
    @PostMapping("/matricula/preview/{idUsuario}")
    fun calcularPreviewMatricula(
        @PathVariable idUsuario: Long,
        @RequestBody request: PagoMatriculaRequest
    ): ResponseEntity<PagoMatriculaPreviewDTO> {
        val preview = pagoService.calcularPreviewMatricula(
            usuarioId = idUsuario,
            alumnoId = request.alumnoId,
            anio = request.anio
        )
        return ResponseEntity.ok(preview)
    }

    // Registrar pago de matrícula (alumno → instituto)
    @PostMapping("/matricula/{idUsuario}")
    fun registrarPagoMatricula(
        @PathVariable idUsuario: Long,
        @RequestBody request: PagoMatriculaRequest
    ): ResponseEntity<PagoDTO> {
        val pago = pagoService.registrarPagoMatricula(
            usuarioId = idUsuario,
            alumnoId = request.alumnoId,
            anio = request.anio
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(pago)
    }

    // Estado de la matrícula de un alumno (pagada / pendiente) para un año
    @GetMapping("/matricula/estado/{alumnoId}")
    fun getEstadoMatricula(
        @PathVariable alumnoId: Long,
        @RequestParam(required = false) anio: Int?
    ): ResponseEntity<EstadoMatriculaDTO> {
        return ResponseEntity.ok(pagoService.getEstadoMatricula(alumnoId, anio))
    }

    // Consultar el monto de matrícula configurado para un año
    @GetMapping("/matricula/config")
    fun getConfiguracionMatricula(
        @RequestParam(required = false) anio: Int?
    ): ResponseEntity<ConfiguracionMatriculaDTO> {
        val anioObjetivo = anio ?: java.time.LocalDate.now().year
        return ResponseEntity.ok(pagoService.getConfiguracionMatricula(anioObjetivo))
    }

    // Configurar el monto de matrícula de un año (solo Admin)
    @PostMapping("/matricula/config/{idUsuario}")
    fun setConfiguracionMatricula(
        @PathVariable idUsuario: Long,
        @RequestBody request: ConfiguracionMatriculaRequest
    ): ResponseEntity<ConfiguracionMatriculaDTO> {
        return ResponseEntity.ok(pagoService.setConfiguracionMatricula(idUsuario, request))
    }

    // Anular un pago (solo Admin)
    @PostMapping("/{pagoId}/anular/{idUsuario}")
    fun anularPago(
        @PathVariable pagoId: Long,
        @PathVariable idUsuario: Long,
        @RequestBody dto: AnularPagoDTO
    ): ResponseEntity<Void> {
        pagoService.anularPago(idUsuario, pagoId, dto)
        return ResponseEntity.noContent().build()
    }

    // Obtener pagos de un curso específico
    // - Si es ALQUILER: Pagos de alquiler del curso
    // - Si es COMISION: Pagos de comisión del curso
    @GetMapping("/curso/{cursoId}")
    fun getPagosPorCurso(
        @PathVariable cursoId: Long
    ): ResponseEntity<List<PagoDTO>> {
        val pagos = pagoService.getPagosPorCurso(cursoId)
        return ResponseEntity.ok(pagos)
    }
}