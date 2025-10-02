package com.estonianport.centro_sis.common.mqtt

import org.springframework.stereotype.Service

@Service
class MqttSubscriberService(
/*mqttClient: MqttClient,
private val plaquetaRepository: PlaquetaRepository,
private val mqttPublisherService: MqttPublisherService*/
) {
/*
private val mqttListener = IMqttMessageListener { topic, message ->
    val payload = String(message.payload)
    println("Mensaje recibido en $topic: $payload")

    when (topic) {
        "plaquetas/registro" -> procesarRegistro(payload)
        "plaquetas/lectura" -> procesarLectura(payload)
        "plaquetas/estado" -> procesarEstado(payload)
    }
}

private fun procesarRegistro(payload: String) {
    val patente = extractPatente(payload)
    val plaqueta = plaquetaRepository.findByPatenteAndEstado(patente, EstadoType.PENDIENTE)
    if (plaqueta != null) {
        plaqueta.estado = EstadoType.ACTIVO
        plaquetaRepository.save(plaqueta)
        println("Plaqueta $patente activada")
        mqttPublisherService.sendCommand(plaqueta.patente, "confirmacion_activacion")
    }
}

private fun procesarLectura(payload: String) {
    try {
        val mapper = jacksonObjectMapper()
        val dto = mapper.readValue(payload, LecturaResponseDto::class.java)

        val piscina = piscinaRepository.findByPlaqueta(
            plaquetaRepository.findByPatenteAndEstado(dto.patente, EstadoType.ACTIVO)!!
        )

        val lectura = Lectura(
            piscina = piscina,
            redox = dto.redox,
            ph = dto.ph,
            cloro = dto.cloro,
            presion = dto.presion,
            temperaturaAgua = dto.temperaturaAgua,
        )

        lecturaRepository.save(lectura)
        println("Lectura guardada correctamente: $lectura")
    } catch (ex: Exception) {
        println("Error parseando lectura: $payload - ${ex.message}")
    }
}

private fun procesarEstado(payload: String) {
    try {
        val mapper = jacksonObjectMapper()
        val dto = mapper.readValue(payload, PiscinaEstadoDto::class.java)

        val piscina = piscinaRepository.findByPlaqueta(
            plaquetaRepository.findByPatenteAndEstado(dto.patente, EstadoType.ACTIVO)!!
        )

        val estado = EstadoPiscina(
            piscina = piscina,
            entradaAguaActiva = dto.entradaAguaActiva.toMutableList(),
            sistemaGermicidaActivo = dto.sistemaGermicidaActivo,
            funcionFiltroActivo = dto.funcionFiltroActivo,
            calefaccionActiva = dto.calefaccionActiva
        )

        estadoPiscinaRepository.save(estado)
        println("Estado guardado correctamente: $estado")
    } catch (ex: Exception) {
        println("Error parseando estado: $payload - ${ex.message}")
    }
}

private fun extractPatente(payload: String): String {
    return Regex("\"patente\":\\s*\"(\\w{4})\"")
        .find(payload)?.groups?.get(1)?.value ?: "UNKNOWN"
}

init {
    mqttClient.subscribe("plaquetas/registro", mqttListener)
    mqttClient.subscribe("plaquetas/lectura", mqttListener)
    mqttClient.subscribe("plaquetas/estado", mqttListener)
    println("Suscrito a topics de plaquetas")
}*/
}