package com.estonianport.centro_sis.model.enums

import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Rol
import com.estonianport.centro_sis.model.RolAdmin
import com.estonianport.centro_sis.model.RolAlumno
import com.estonianport.centro_sis.model.RolOficina
import com.estonianport.centro_sis.model.RolPorteria
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.Usuario

enum class RolType {
    ADMINISTRADOR {
        override fun create(usuario: Usuario): Rol = RolAdmin(usuario)
    },
    OFICINA {
        override fun create(usuario: Usuario): Rol = RolOficina(usuario)
    },
    PROFESOR {
        override fun create(usuario: Usuario): Rol = RolProfesor(usuario)
    },
    ALUMNO {
        override fun create(usuario: Usuario): Rol = RolAlumno(usuario)
    },
    PORTERIA
    {
        override fun create(usuario: Usuario): Rol = RolPorteria(usuario)
    };

    abstract fun create(usuario: Usuario): Rol

    companion object {
        fun fromString(value: String): RolType =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Rol desconocido: $value")
    }
}