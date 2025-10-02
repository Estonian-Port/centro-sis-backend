package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.Usuario

interface BeneficioStrategy {
    fun aplicarBeneficio(arancelBase: Double, usuario: Usuario, curso: Curso): Double
}

class DescuentoPorFamilia : BeneficioStrategy {
    override fun aplicarBeneficio(arancelBase: Double, usuario: Usuario, curso: Curso): Double {
        // Ejemplo: 15% de descuento por familia
        return arancelBase * 0.85
    }
}

class DescuentoPorBeca : BeneficioStrategy {
    override fun aplicarBeneficio(arancelBase: Double, usuario: Usuario, curso: Curso): Double {
        // Ejemplo: 30% de descuento por beca
        return arancelBase * 0.70
    }
}