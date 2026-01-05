package com.estonianport.centro_sis.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_pago")
abstract class Pago(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    val monto: BigDecimal,

    @Column(nullable = false)
    val fecha: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null,

    @Column(length = 500)
    var observaciones: String? = null
) {
    fun anular(motivo: String) {
        this.fechaBaja = LocalDate.now()
        this.observaciones = "ANULADO: $motivo"
    }

    fun estaActivo(): Boolean = fechaBaja == null
}

// Pago de alumno por inscripción
@Entity
@DiscriminatorValue("CURSO")
class PagoCurso(
    monto: BigDecimal,
    fecha: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val inscripcion: Inscripcion,

    @Column(nullable = false)
    val retraso: Boolean = false,

    @Column(nullable = false)
    val beneficioAplicado: Int = 0
) : Pago(monto = monto, fecha = fecha) {

    fun getAlumno(): RolAlumno = inscripcion.alumno
    fun getCurso(): Curso = inscripcion.curso

    fun calcularDescuentoAplicado(): BigDecimal {
        return inscripcion.tipoPagoSeleccionado.monto * (BigDecimal(beneficioAplicado) / BigDecimal(100))
    }
}

// Pago de profesor al instituto (alquiler)
@Entity
@DiscriminatorValue("ALQUILER")
class PagoAlquiler(
    monto: BigDecimal,
    fecha: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val curso: CursoAlquiler,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val profesor: RolProfesor
) : Pago(monto = monto, fecha = fecha)

// Pago del instituto a profesor (comisión)
@Entity
@DiscriminatorValue("COMISION")
class PagoComision(
    monto: BigDecimal,
    fecha: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val curso: CursoComision,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val profesor: RolProfesor
) : Pago(monto = monto, fecha = fecha)