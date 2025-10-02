package com.estonianport.centro_sis.common.security

import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity // Importante para habilitar la seguridad web
class SecurityConfig(
        private val jwtAuthorizationFilter: JWTAuthorizationFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity, authenticationManager: AuthenticationManager, usuarioService: UsuarioService): SecurityFilterChain {
        val jwtAuthenticationFilter = JWTAuthenticationFilter(usuarioService)
        jwtAuthenticationFilter.setAuthenticationManager(authenticationManager)
        jwtAuthenticationFilter.setFilterProcessesUrl("/login")
        jwtAuthenticationFilter.setAuthenticationFailureHandler(CustomAuthenticationFailureHandler())

        return http
                .cors(Customizer.withDefaults())
                .csrf { csrf -> csrf.disable() }
                .authorizeHttpRequests { authorize ->
                    authorize
                            .requestMatchers("/", "/index.html", "/styles.css", "/logo.png", "/favicon.ico").permitAll()
                            .requestMatchers("/actuator/prometheus").access(WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                            .anyRequest().authenticated()
                }
                .sessionManagement { session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                .addFilter(jwtAuthenticationFilter)
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter::class.java)
                .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // Nuevo Bean para el AuthenticationManager usando AuthenticationConfiguration
    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
}
