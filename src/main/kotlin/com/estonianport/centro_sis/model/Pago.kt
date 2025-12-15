package com.estonianport.centro_sis.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "pagos")
class Pago(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var inscripcion: Inscripcion,

    @Column(nullable = false, precision = 10, scale = 2)
    var monto: BigDecimal,

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val retraso: Boolean = false,

    @Column(nullable = false, precision = 5, scale = 2)
    val beneficioAplicado: BigDecimal = inscripcion.beneficio,

    @Column
    var fechaBaja: LocalDate? = null,

    @Column(length = 500)
    var observaciones: String? = null
) {

    init {
        require(monto > BigDecimal.ZERO) {
            "El monto debe ser mayor a cero"
        }
        require(beneficioAplicado >= BigDecimal.ZERO && beneficioAplicado <= BigDecimal.ONE) {
            "El beneficio aplicado debe estar entre 0 y 1"
        }
    }

    fun getAlumno(): RolAlumno = inscripcion.alumno

    fun getCurso(): Curso = inscripcion.curso

    fun anular(motivo: String) {
        this.fechaBaja = LocalDate.now()
        this.observaciones = "ANULADO: $motivo"
    }

    fun estaActivo(): Boolean = fechaBaja == null

    fun calcularDescuentoAplicado(): BigDecimal {
        return inscripcion.tipoPago.monto * beneficioAplicado
    }

    fun getPorcentajeDescuento(): Int {
        val descuento = BigDecimal.ONE - beneficioAplicado
        return (descuento * BigDecimal(100)).toInt()
    }
}