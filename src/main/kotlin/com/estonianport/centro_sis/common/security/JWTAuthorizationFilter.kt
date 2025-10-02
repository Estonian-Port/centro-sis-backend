package com.estonianport.unique.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JWTAuthorizationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val bearerToken : String? = request.getHeader("Authorization")

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            val token : String = bearerToken.replace("Bearer ", "")
            val usernamePAT : UsernamePasswordAuthenticationToken? = TokenUtils.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = usernamePAT
        }

        filterChain.doFilter(request, response)
    }

}