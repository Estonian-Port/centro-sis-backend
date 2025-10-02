package com.estonianport.unique.common.security

import com.estonianport.unique.model.Usuario
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

class UserDetailImpl(var usuario : Usuario) : UserDetails{

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return Collections.emptyList()
    }

    override fun getPassword(): String? {
        return usuario.password
    }

    override fun getUsername(): String? {
        return usuario.email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return usuario.fechaBaja == null
    }

    fun getNombre() : String{
        return usuario.nombre
    }
}