package com.estonianport.centro_sis.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.util.*


class TokenUtils {

    companion object{

        private val ACCESS_TOKEN_SECRET: String =
            System.getenv("JWT_SECRET")
        const val ACCESS_TOKEN_VALIDITY_SECONDS: Long = 5184000

        fun createToken(nombre: String, username: String?): String {
            val expirationDate : Date = Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_SECONDS)

            val extra: MutableMap<String, Any> = HashMap()
            extra["nombre"] = nombre

            return Jwts.builder()
                .setSubject(username)
                .setExpiration(expirationDate)
                .addClaims(extra)
                .signWith(Keys.hmacShaKeyFor((ACCESS_TOKEN_SECRET.toByteArray())))
                .compact()
        }

        fun getAuthentication(token : String) : UsernamePasswordAuthenticationToken? {

            try {
                val claims : Claims = Jwts.parserBuilder()
                    .setSigningKey(ACCESS_TOKEN_SECRET.toByteArray())
                    .build()
                    .parseClaimsJws(token)
                    .body

                val username : String = claims.subject

                return UsernamePasswordAuthenticationToken(username, null, Collections.emptyList())
            }catch (e : JwtException){
                return null
            }

        }
    }
}