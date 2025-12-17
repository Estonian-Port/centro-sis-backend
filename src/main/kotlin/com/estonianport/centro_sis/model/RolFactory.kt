package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.RolType
import org.springframework.stereotype.Service

@Service
class RolFactory {
    fun build(roleString: String, usuario: Usuario): Rol {
        val tipo = RolType.fromString(roleString)
        return tipo.create(usuario)
    }
}