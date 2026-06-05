package com.estonianport.centro_sis.common.cache

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule // <-- NUEVA IMPORTACIÓN
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val objectMapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())

            activateDefaultTyping(
                polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        }

        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build()
    }
}