package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.BeneficioType
import org.springframework.stereotype.Component

@Component
class BeneficioFactory {

    fun getStrategy(tipo: BeneficioType): BeneficioStrategy? {
        return when (tipo) {
            BeneficioType.FAMILIA -> DescuentoPorFamilia()
            BeneficioType.BECA -> DescuentoPorBeca()
            // Agrega más casos según tus enums
            else -> null
        }
    }
}