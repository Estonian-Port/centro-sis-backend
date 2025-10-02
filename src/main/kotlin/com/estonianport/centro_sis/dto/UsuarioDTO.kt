package com.estonianport.centro_sis.dto

import java.time.LocalDate


class UsuarioPerfilDTO(val id: Long, var nombre: String, val apellido: String, val username: String, val email: String, val celular : Long, val fechaNacimiento : LocalDate)

class UsuarioAbmDTO(var id: Long, var nombre: String, var apellido: String) {}

class UsuarioEditPasswordDTO(var id : Long, var password: String) {}
