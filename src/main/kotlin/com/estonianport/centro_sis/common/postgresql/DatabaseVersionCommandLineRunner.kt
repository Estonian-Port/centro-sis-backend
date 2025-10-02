package com.estonianport.centro_sis.common.postgresql

import org.springframework.boot.CommandLineRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseVersionCommandLineRunner(
        private val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        println(jdbcTemplate.queryForObject("SELECT version();", String::class.java))
    }
}
