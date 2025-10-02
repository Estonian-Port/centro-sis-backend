package com.estonianport.centro_sis.common.security

import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.repository.UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailServiceImpl : UserDetailsService {

    @Autowired
    lateinit var usuarioRepository : UsuarioRepository

    override fun loadUserByUsername(username: String): UserDetails{

        val usuario : Usuario = usuarioRepository.findOneByEmail(username)
            ?: throw UsernameNotFoundException("No se encontró el usuario")

        return UserDetailImpl(usuario)
    }

}