package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.EstadoPagoType
import com.estonianport.centro_sis.model.enums.RolType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Entity
class Inscripcion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var alumno: RolAlumno,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var curso: Curso,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "tipoPago", column = Column(name = "tipo_pago")),
        AttributeOverride(name = "monto", column = Column(name = "monto_tipo_pago"))
    )
    var tipoPagoSeleccionado: TipoPago,

    @Column(nullable = false)
    var fechaInscripcion: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null,

    @OneToMany(mappedBy = "inscripcion", cascade = [CascadeType.ALL], orphanRemoval = true)
    var pagos: MutableList<PagoCurso> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estadoPago: EstadoPagoType = EstadoPagoType.PENDIENTE,

    @Column(nullable = false)
    var beneficio: Int = 0
) {

    init {
        require(beneficio in 0..100) {
            "El porcentaje de descuento debe estar entre 0 y 100"
        }
    }

    // Monto normal sin recargo
    fun calcularMontoBase(): BigDecimal {
        return tipoPagoSeleccionado.monto * convertirBeneficio()
    }

    // Monto con recargo si está atrasado
    fun calcularArancelFinal(): BigDecimal {
        return calcularMontoBase() * verificarRetraso()
    }

    fun estaAlDia(): Boolean {
        return when (tipoPagoSeleccionado.tipo) {
            PagoType.MENSUAL -> estaAlDiaMensual()
            PagoType.TOTAL -> estaAlDiaAnual()
        }
    }

    private fun estaAlDiaMensual(): Boolean {
        val mesesDesdeInscripcion = calcularMesesDesdeInscripcion()
        val pagosRealizados = pagos.count { it.fechaBaja == null }
        return pagosRealizados >= mesesDesdeInscripcion
    }

    private fun estaAlDiaAnual(): Boolean {
        // Si pagó el año completo, está al día. O sea registra un pago activo.
        return pagos.any { it.fechaBaja == null }
    }

    private fun calcularMesesDesdeInscripcion(): Int {
        val inicio = YearMonth.from(curso.fechaInicio)
        val actual = YearMonth.now()

        // Si la fecha de inicio es futura, retorna 0
        if (inicio.isAfter(actual)) return 0

        var meses = 0
        var temp = inicio

        while (temp.isBefore(actual) || temp == actual) {
            meses++
            temp = temp.plusMonths(1)
        }

        return meses
    }

    fun registrarPago(): PagoCurso {
        val pago = PagoCurso(
            inscripcion = this,
            monto = calcularArancelFinal(),
            fecha = LocalDate.now(),
            retraso = esDeudor(),
            beneficioAplicado = beneficio
        )

        pagos.add(pago)
        actualizarEstadoPago()

        return pago
    }

    private fun actualizarEstadoPago() {
        estadoPago = when {
            estaAlDia() -> EstadoPagoType.AL_DIA
            esDeudor() -> EstadoPagoType.ATRASADO
            else -> EstadoPagoType.PENDIENTE
        }
    }

    private fun fueDeudor(): Boolean {
        return pagos.any { it.retraso && it.fechaBaja == null }
    }

    private fun verificarRetraso(): BigDecimal {
        return if (esDeudor()) {
            curso.recargoAtraso
        } else {
            BigDecimal.ONE
        }
    }

    private fun esDeudor(): Boolean {
        val mesesDesdeInscripcion = calcularMesesDesdeInscripcion()
        val pagosRealizados = pagos.count { it.fechaBaja == null }
        val pagosAdeudados = mesesDesdeInscripcion - pagosRealizados
        if (pagosAdeudados == 0) return false // si es 0 no debe nada
        if (pagosAdeudados > 1) return true // si debe más de un mes, seguro debe el último mes
        return verificarFechaPago() // si debe un mes, verificar si ya pasó el plazo
    }

    private fun verificarFechaPago(): Boolean {
        val fechaActual = LocalDate.now()
        if (fechaActual.dayOfMonth <= 10) {
            return false // Aún está dentro del plazo para pagar el último mes
        }
        return true // Ya pasó el plazo, debe el último mes
    }

    fun calcularDeudaPendiente(): BigDecimal {
        val pagosEsperados = when (tipoPagoSeleccionado.tipo) {
            PagoType.MENSUAL -> calcularMesesDesdeInscripcion()
            PagoType.TOTAL -> 1
        }

        val pagosRealizados = pagos.count { it.fechaBaja == null }
        val pagosPendientes = maxOf(0, pagosEsperados - pagosRealizados)

        return BigDecimal(pagosPendientes) * calcularArancelFinal()
    }

    fun convertirBeneficio(): BigDecimal {
        // Convertir porcentaje a multiplicador
        // Ej: 20% descuento = 0.80 (paga el 80%)
        val beneficioAplicado = BigDecimal.ONE - (BigDecimal(beneficio) / BigDecimal(100))
        return beneficioAplicado
    }

    fun aplicarBeneficio(nuevoBeneficio: Int) {
        require(nuevoBeneficio in 0..100) {
            "El porcentaje de descuento debe estar entre 0 y 100"
        }
        beneficio = nuevoBeneficio
    }

    fun quitarBeneficio() {
        beneficio = 0
    }

    fun darDeBaja(fecha: LocalDate = LocalDate.now()) {
        this.fechaBaja = fecha
    }

    fun puedeEditar(usuario: Usuario) {
        if (!usuario.tieneRol(RolType.ADMINISTRADOR) || usuario.tieneRol(RolType.PROFESOR)) {
            throw IllegalAccessException("El usuario con id ${usuario.id} no tiene permiso para editar esta inscripción")
        }
    }
}