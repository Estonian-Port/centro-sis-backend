package com.estonianport.centro_sis.common.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mqtt")
class MqttProperties {
    lateinit var brokerUrl: String
    lateinit var username: String
    lateinit var password: String
}