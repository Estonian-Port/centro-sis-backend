package com.estonianport.centro_sis.common.mqtt

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.stereotype.Service

@Service
class MqttPublisherService(/*private val mqttClient: MqttClient*/) {

    /*fun sendCommand(patente: String, accion: String) {
        val topic = "plaquetas/comando"
        val idSolicitud = System.currentTimeMillis()
        val payload =
            """{
                "patente": "$patente",
                "id_solicitud": $idSolicitud,
                "accion": "$accion"
            }"""
        val message = MqttMessage(payload.toByteArray()).apply { qos = 1 }
        mqttClient.publish(topic, message)
        println("Publicado comando $accion para plaqueta $patente en $topic")
    }

    fun sendCommandList(
        patente: String,
        entradasAgua: List<EntradaAguaType> = emptyList(),
        funcionFiltro: FuncionFiltroType? = null,
        sistemasGermicida: List<SistemaGermicidaType> = emptyList()
    ) {
        val topic = "plaquetas/control"
        val idSolicitud = System.currentTimeMillis()

        val payloadMap = mutableMapOf(
            "patente" to patente,
            "id_solicitud" to idSolicitud,
            "entrada_agua" to entradasAgua,
            "funcion_filtro" to (funcionFiltro ?: ""),
            "sistema_germicida" to sistemasGermicida
        )

        val payloadJson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(payloadMap)
        val message = MqttMessage(payloadJson.toByteArray()).apply { qos = 1 }
        mqttClient.publish(topic, message)
        println("Publicado comando lista para plaqueta $patente en $topic: $payloadJson")
    }*/
}