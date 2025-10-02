package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.model.BeneficioFactory
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Usuario
import org.springframework.stereotype.Service

@Service
class BeneficioService(
    private val beneficioFactory: BeneficioFactory
) {

    fun calcularArancelConBeneficios(usuario: Usuario, curso: Curso): Double {
        var arancelFinal = curso.arancel

        usuario.beneficios.forEach { tipoBeneficio ->
            val strategy = beneficioFactory.getStrategy(tipoBeneficio)
            strategy?.let {
                arancelFinal = it.aplicarBeneficio(arancelFinal, usuario, curso)
            }
        }

        return arancelFinal
    }
}