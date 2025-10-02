package com.estonianport.centro_sis.common.mqtt

import com.estonianport.unique.model.ErrorLectura
import com.estonianport.unique.model.enums.EstadoType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MqttSchedulerService(
    /*private val plaquetaRepository: PlaquetaRepository,*/
) {
/*
    @Scheduled(fixedRate = 30 * 60 * 1000)
    fun verificarLecturas() {
        val ahora = LocalDateTime.now()
        val limite = ahora.minusMinutes(35)

        val plaquetas = plaquetaRepository.findByEstado(EstadoType.ACTIVO)
        plaquetas?.forEach { plaqueta ->
            val ultima = lecturaRepository.findUltimaByPlaqueta(plaqueta.patente)

            if (ultima == null || ultima.fecha.isBefore(limite)) {
                errorLecturaRepository.save(ErrorLectura(piscinaRepository.findByPlaqueta(plaqueta)))
                println("Guardado ErrorLectura para plaqueta ${plaqueta.id}")
            }
        }
    }*/
}
