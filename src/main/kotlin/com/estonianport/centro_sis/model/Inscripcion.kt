package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "inscripcion")
class Inscripcion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    var alumno: RolAlumno,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    var curso: Curso,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "tipo", column = Column(name = "tipo_pago")),
        AttributeOverride(name = "monto", column = Column(name = "monto_tipo_pago")),
        AttributeOverride(name = "cuotas", column = Column(name = "cuotas"))
    )
    var tipoPagoSeleccionado: TipoPago,

    @Column(nullable = false)
    var fechaInscripcion: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null,

    @OneToMany(
        mappedBy = "inscripcion",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var pagos: MutableList<PagoCurso> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estadoPago: EstadoPagoType = EstadoPagoType.PENDIENTE,

    // Porcentaje de descuento (0-100)
    @Column(nullable = false)
    var beneficio: Int = 0,

    @Column(nullable = false)
    var puntos: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoType = EstadoType.ACTIVO
) {

    init {
        require(beneficio in 0..100) {
            "El porcentaje de descuento debe estar entre 0 y 100"
        }
    }

    /**
     * Registra un pago. El recargo se controla manualmente desde el frontend.
     * @param aplicarRecargo: true si el profesor/admin decide cobrar recargo
     */
    fun registrarPago(
        registradoPor: Usuario,
        aplicarRecargo: Boolean = false
    ): PagoCurso {
        require(puedeRegistrarPago()) {
            "No se pueden registrar más pagos. La inscripción está completa o dada de baja."
        }

        val montoPorCuota = calcularMontoPorCuota()

        val montoFinal = if (aplicarRecargo) {
            montoPorCuota * curso.recargoAtraso
        } else {
            montoPorCuota
        }

        val pago = PagoCurso(
            monto = montoFinal,
            registradoPor = registradoPor,
            inscripcion = this,
            conRecargo = aplicarRecargo,
            beneficioAplicado = beneficio
        )

        pagos.add(pago)
        actualizarEstadoPago()

        return pago
    }

    // Calcula cuánto cuesta cada cuota (ya con descuento)
    fun calcularMontoPorCuota(): BigDecimal {
        val montoTotal = tipoPagoSeleccionado.monto
        val descuento = (montoTotal * BigDecimal(beneficio)) / BigDecimal(100)
        val montoConDescuento = montoTotal - descuento

        return montoConDescuento
    }

    fun calcularDescuentoAplicado(): BigDecimal {
        val montoTotal = tipoPagoSeleccionado.monto
        val descuento = (montoTotal * BigDecimal(beneficio)) / BigDecimal(100)
        return descuento
    }

    fun calcularRecargoAplicado(): BigDecimal {
        val recargo = tipoPagoSeleccionado.monto * (curso.recargoAtraso / BigDecimal(100))
        return recargo
    }

    fun calcularMontoFinalConRecargo(): BigDecimal {
        val montoConDescuento = calcularMontoPorCuota()
        val recargo = calcularRecargoAplicado()
        return montoConDescuento + recargo
    }

    // Calcula cuántas cuotas DEBERÍA haber pagado hasta ahora
    // según el tiempo transcurrido desde el inicio del curso.

    fun calcularCuotasEsperadas(): Int {
        return when (tipoPagoSeleccionado.tipo) {
            PagoType.MENSUAL -> calcularCuotasEsperadasMensuales()
            PagoType.TOTAL -> calcularCuotasEsperadasTotal()
        }
    }


    // Para pago mensual: 1 cuota por cada mes transcurrido desde el inicio

    private fun calcularCuotasEsperadasMensuales(): Int {
        val hoy = LocalDate.now()
        val inicioCurso = curso.fechaInicio

        // Si el curso no empezó, no se espera ninguna cuota
        if (hoy.isBefore(inicioCurso)) return 0

        // Si el curso terminó, se esperan todas las cuotas
        if (hoy.isAfter(curso.fechaFin)) {
            return tipoPagoSeleccionado.cuotas
        }

        // Calcular meses transcurridos COMPLETOS
        val mesesTranscurridos = ChronoUnit.MONTHS.between(
            YearMonth.from(inicioCurso),
            YearMonth.from(hoy)
        ).toInt() + 1 // +1 porque el mes actual ya empezó

        // No puede superar el total de cuotas
        return minOf(mesesTranscurridos, tipoPagoSeleccionado.cuotas)
    }

    // Para pago total: se espera 1 cuota desde el inicio del curso
    private fun calcularCuotasEsperadasTotal(): Int {
        val hoy = LocalDate.now()
        val inicioCurso = curso.fechaInicio

        return if (hoy.isBefore(inicioCurso)) 0 else 1
    }

    // Actualiza el estado considerando si está atrasado
    fun actualizarEstadoPago() {
        val cuotasPagadas = contarCuotasPagadas()
        val cuotasEsperadas = calcularCuotasEsperadas()
        val cuotasTotales = tipoPagoSeleccionado.cuotas

        estadoPago = when {
            // Pagó todas las cuotas
            cuotasPagadas >= cuotasTotales -> EstadoPagoType.PAGO_COMPLETO

            // Está atrasado (debe más de lo que pagó)
            cuotasPagadas < cuotasEsperadas -> EstadoPagoType.ATRASADO

            // Al día (pagó lo que debía hasta ahora)
            cuotasPagadas >= cuotasEsperadas && cuotasPagadas > 0 -> EstadoPagoType.AL_DIA

            // Pendiente (no pagó nada pero tampoco debe aún)
            else -> EstadoPagoType.PENDIENTE
        }
    }

    // Verifica si está atrasado con los pagos
    fun estaAtrasado(): Boolean {
        return contarCuotasPagadas() < calcularCuotasEsperadas()
    }

    // Calcula cuántas cuotas debe (no pagadas pero esperadas)
    fun calcularCuotasAtrasadas(): Int {
        val cuotasPagadas = contarCuotasPagadas()
        val cuotasEsperadas = calcularCuotasEsperadas()
        return maxOf(0, cuotasEsperadas - cuotasPagadas)
    }

    // Cuenta cuántas cuotas pagó (solo pagos activos)
    fun contarCuotasPagadas(): Int {
        return pagos.count { it.fechaBaja == null }
    }

    // Calcula deuda pendiente simple
    fun calcularDeudaPendiente(): BigDecimal {
        val cuotasPendientes = calcularCuotasEsperadas() - contarCuotasPagadas()
        val deudaTotal = calcularMontoPorCuota().multiply(BigDecimal(cuotasPendientes))
        return maxOf(BigDecimal.ZERO, deudaTotal)
    }

    // Total pagado hasta ahora
    fun calcularTotalPagado(): BigDecimal {
        return pagos
            .filter { it.fechaBaja == null }
            .sumOf { it.monto }
    }

    // Verifica si puede registrar más pagos
    fun puedeRegistrarPago(): Boolean {
        return estadoPago != EstadoPagoType.PAGO_COMPLETO &&
                fechaBaja == null &&
                contarCuotasPagadas() < tipoPagoSeleccionado.cuotas
    }

    // Anula un pago (solo admin)
    fun anularPago(pago: PagoCurso, motivo: String, anulador: Usuario) {
        require(pagos.contains(pago)) {
            "El pago no pertenece a esta inscripción"
        }

        pago.anular(motivo, anulador)
        actualizarEstadoPago()
    }

    fun aplicarBeneficio(nuevoBeneficio: Int) {
        require(nuevoBeneficio in 0..100) {
            "El porcentaje de descuento debe estar entre 0 y 100"
        }
        beneficio = nuevoBeneficio
    }

    fun darPuntos(otorgadoPor: Usuario, puntosAGanar: Int) {
        verificarPermisoEdicion(otorgadoPor)
        require(puntosAGanar > 0) { "Los puntos deben ser positivos" }
        puntos += puntosAGanar
    }

    fun verificarPermisoEdicion(usuario: Usuario) {
        val tienePermiso = usuario.tieneRol(RolType.ADMINISTRADOR) ||
                usuario.tieneRol(RolType.OFICINA) ||
                usuario.tieneRol(RolType.PROFESOR)

        require(tienePermiso) {
            "El usuario ${usuario.nombreCompleto()} no tiene permiso para editar esta inscripción"
        }
    }

    fun darDeBaja(fecha: LocalDate = LocalDate.now()) {
        this.fechaBaja = fecha
        this.estado = EstadoType.BAJA
    }

    fun reactivar() {
        require(estado == EstadoType.BAJA) {
            "Solo se pueden reactivar inscripciones dadas de baja"
        }
        this.fechaBaja = null
        this.estado = EstadoType.ACTIVO
        actualizarEstadoPago()
    }

    fun obtenerResumenPago(): ResumenPago {
        val cuotasPagadas = contarCuotasPagadas()
        val cuotasEsperadas = calcularCuotasEsperadas()
        val cuotasAtrasadas = calcularCuotasAtrasadas()
        val cuotasPendientes = tipoPagoSeleccionado.cuotas - cuotasPagadas
        val deudaTotal = calcularDeudaPendiente()
        val totalPagado = calcularTotalPagado()

        return ResumenPago(
            cuotasTotales = tipoPagoSeleccionado.cuotas,
            cuotasPagadas = cuotasPagadas,
            cuotasEsperadas = cuotasEsperadas,
            cuotasAtrasadas = cuotasAtrasadas,
            cuotasPendientes = cuotasPendientes,
            deudaTotal = deudaTotal,
            totalPagado = totalPagado,
            estadoActual = estadoPago,
            beneficioAplicado = beneficio,
            montoPorCuota = calcularMontoPorCuota()
        )
    }
}

data class ResumenPago(
    val cuotasTotales: Int,
    val cuotasPagadas: Int,
    val cuotasEsperadas: Int,
    val cuotasAtrasadas: Int,
    val cuotasPendientes: Int,
    val deudaTotal: BigDecimal,
    val totalPagado: BigDecimal,
    val estadoActual: EstadoPagoType,
    val beneficioAplicado: Int,
    val montoPorCuota: BigDecimal
)