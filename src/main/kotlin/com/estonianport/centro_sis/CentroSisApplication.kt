package com.estonianport.centro_sis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableCaching
class CentroSisApplication

fun main(args: Array<String>) {
	runApplication<CentroSisApplication>(*args)
}
