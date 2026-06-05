package com.estonianport.centro_sis.model

import jakarta.persistence.*
import java.math.BigDecimal

// ========================================
// Monto global de la matrícula para un año.
// Hay como máximo una configuración por año.
// ========================================
@Entity
@Table(name = "configuracion_matricula")
class ConfiguracionMatricula(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val anio: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    var monto: BigDecimal
) {
    init {
        require(monto > BigDecimal.ZERO) {
            "El monto de la matrícula debe ser mayor a cero"
        }
    }
}
