package com.estonianport.centro_sis.common.security

import com.estonianport.centro_sis.common.security.AuthCredentials
import com.estonianport.centro_sis.common.security.UserDetailImpl
import com.estonianport.centro_sis.service.UsuarioService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

class JWTAuthenticationFilter(
    private val usuarioService: UsuarioService
) : UsernamePasswordAuthenticationFilter() {

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse?): Authentication? {
        var authCredentials = AuthCredentials()

        try {
            authCredentials = ObjectMapper().readValue(
                request.reader,
                AuthCredentials::class.java
            )
        } catch (_: IOException) {
        }
        val usernamePAT = UsernamePasswordAuthenticationToken(
            authCredentials.username,
            authCredentials.password,
            emptyList()
        )
        return authenticationManager.authenticate(usernamePAT)
    }

    override fun successfulAuthentication(request: HttpServletRequest?, response: HttpServletResponse?, chain: FilterChain?, authResult: Authentication?) {
        val email = authResult?.name ?: return
        usuarioService.actualizarFechaUltimoAcceso(email, LocalDateTime.now())

        val userDetails : UserDetailImpl = authResult.principal as UserDetailImpl

        val token : String = TokenUtils.createToken(userDetails.getNombre(), userDetails.username)

        if (response != null) {
            response.addHeader("Authorization", "Bearer $token")
            response.writer.flush()
        }

        super.successfulAuthentication(request, response, chain, authResult)
    }

}