package com.estonianport.centro_sis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CentroSisApplication

fun main(args: Array<String>) {
	runApplication<CentroSisApplication>(*args)
}
