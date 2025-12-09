package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.EstadoPagoType
import jakarta.persistence.*
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tipoPago: PagoType,

    //Puede un alumno anotarse una vez iniciado el curso? en ese caso pago la totalidad del mismo?
    @Column(nullable = false)
    var fechaInicioCurso: LocalDate = curso.fechaInicio,

    @Column
    var fechaBaja: LocalDate? = null,

    @OneToMany(mappedBy = "inscripcion", cascade = [CascadeType.ALL], orphanRemoval = true)
    var pagos: MutableList<Pago> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estadoPago: EstadoPagoType = EstadoPagoType.PENDIENTE
) {

    fun calcularArancelFinal(beneficioFactory: BeneficioFactory): Double {
        val arancelBase = curso.arancel
        val arancelConRecargo = aplicarRecargoPorTipoPago(arancelBase)

        // Aplicar beneficios del alumno sobre el arancel con recargo
        return alumno.calcularArancelFinal(arancelConRecargo, beneficioFactory)
    }

    // PREGUNTAR SI EL CALCULO ES IGUAL PARA TODOS LOS CURSOS POR IGUAL
    private fun aplicarRecargoPorTipoPago(arancelBase: Double): Double {
        return when (tipoPago) {
            PagoType.MENSUAL -> arancelBase * 1.0  // Sin recargo
            PagoType.TOTAL -> arancelBase * 0.90  // 10% descuento por pago completo
        }
    }

    fun estaAlDia(): Boolean {
        return when (tipoPago) {
            PagoType.MENSUAL -> estaAlDiaMensual()
            PagoType.TOTAL -> estaAlDiaAnual()
        }
    }

    private fun estaAlDiaMensual(): Boolean {
        val mesActual = YearMonth.now()
        val mesesDesdeInscripcion = calcularMesesDesdeInscripcion()

        // Cuántos pagos debería tener hasta ahora
        val pagosEsperados = mesesDesdeInscripcion

        // Cuántos pagos tiene realmente
        val pagosRealizados = pagos.count { it.fechaBaja == null }

        return pagosRealizados >= pagosEsperados
    }

    private fun estaAlDiaAnual(): Boolean {
        // Si pagó el año completo, está al día
        return pagos.any { it.fechaBaja == null }
    }

    private fun calcularMesesDesdeInscripcion(): Int {
        val inicio = YearMonth.from(fechaInicioCurso)
        val actual = YearMonth.now()

        var meses = 0
        var temp = inicio

        while (temp.isBefore(actual) || temp == actual) {
            meses++
            temp = temp.plusMonths(1)
        }

        return meses
    }

    fun obtenerProximoMonto(beneficioFactory: BeneficioFactory): Double {
        val arancelFinal = calcularArancelFinal(beneficioFactory)

        return when (tipoPago) {
            PagoType.MENSUAL -> arancelFinal
            PagoType.TOTAL -> arancelFinal * 12
        }
    }

    fun registrarPago(monto: Double, fecha: LocalDate = LocalDate.now(), conRetraso: Boolean = false): Pago {
        val pago = Pago(
            inscripcion = this,
            monto = monto,
            fecha = fecha,
            retraso = conRetraso
        )

        pagos.add(pago)
        actualizarEstadoPago()

        return pago
    }

    private fun actualizarEstadoPago() {
        estadoPago = when {
            estaAlDia() -> EstadoPagoType.AL_DIA
            tieneRetrasos() -> EstadoPagoType.ATRASADO
            else -> EstadoPagoType.MOROSO
        }
    }

    private fun tieneRetrasos(): Boolean {
        return pagos.any { it.retraso && it.fechaBaja == null }
    }

    fun calcularDeudaPendiente(beneficioFactory: BeneficioFactory): Double {
        val pagosEsperados = when (tipoPago) {
            PagoType.MENSUAL -> calcularMesesDesdeInscripcion()
            PagoType.TOTAL -> 1
        }

        val pagosRealizados = pagos.count { it.fechaBaja == null }
        val pagosPendientes = maxOf(0, pagosEsperados - pagosRealizados)

        return pagosPendientes * obtenerProximoMonto(beneficioFactory)
    }
}