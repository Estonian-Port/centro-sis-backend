package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.common.emailService.EmailService
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.service.AdministracionService
import com.estonianport.centro_sis.service.UsuarioService
import com.estonianport.centro_sis.dto.request.UsuarioAltaRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRequestDto
import com.estonianport.centro_sis.model.RolFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/usuario")
@CrossOrigin("*")
class UsuarioController (
    private val rolFactory: RolFactory,
) {

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    lateinit var administracionService: AdministracionService

    @Autowired
    lateinit var usuarioService: UsuarioService

    @GetMapping("/me")
    fun getCurrent(principal: Principal): ResponseEntity<CustomResponse> {
        val email = principal.name
        val usuario = usuarioService.getUsuarioByEmail(email)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario obtenido correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    @DeleteMapping("delete/{usuarioId}/{administradorId}")
    fun delete(@PathVariable usuarioId: Long, @PathVariable administradorId: Long): ResponseEntity<CustomResponse> {
        administracionService.verificarRol(administradorId)
        usuarioService.darDeBaja(usuarioId)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario eliminado correctamente",
                data = null
            )
        )
    }

    //Endpoint para las altas de usuarios por parte del administrador
    @PostMapping("/altaUsuario")
    fun altaUsuario(@RequestBody usuarioDto: UsuarioAltaRequestDto): ResponseEntity<CustomResponse> {
        usuarioService.verificarEmailNoExiste(usuarioDto.email)
        val usuario = UsuarioMapper.buildAltaUsuario(usuarioDto)

        val password = usuarioService.generarPassword()
        usuario.password = usuarioService.encriptarPassword(password)
        usuario.fechaAlta = LocalDate.now()
        val rol = rolFactory.build(usuarioDto.rol, usuario)
        usuario.asignarRol(rol)

        usuarioService.save(usuario)

        try {
            emailService.enviarEmailAltaUsuario(usuario, "Bienvenido a CENTRO SIS", password);
        } catch (_: Exception) {
            // TODO enviar notificacion de fallo al enviar el mail
        }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario creado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    @PutMapping("/registro")
    fun registro(@RequestBody usuarioDto: UsuarioRegistroRequestDto): ResponseEntity<CustomResponse> {
        usuarioService.primerLogin(usuarioDto)
        val usuario = usuarioService.findById(usuarioDto.id)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Registro realizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario!!)
            )
        )
    }


    @PostMapping("/save")
    fun save(@RequestBody usuarioDto: UsuarioRequestDto): ResponseEntity<CustomResponse> {
        val usuario = UsuarioMapper.buildUsuario(usuarioDto)

        // Traemos la password del back para que no viaje por temas de seguridad al editar un usuario
        // Para editar password usamos el endpoint especifico /editPassword
        usuario.password = usuarioService.findById(usuarioDto.id)!!.password!!


        usuarioService.save(usuario)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario editado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    // Busca el usuario por id y encriptar la nueva password
    @PostMapping("/update-password")
    fun editPassword(@RequestBody usuarioDto: UsuarioCambioPasswordRequestDto): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Password actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePassword(usuarioDto))
            )
        )
    }

    @PutMapping("/update-perfil")
    fun updatePerfil(@RequestBody usuarioDto: UsuarioRequestDto): ResponseEntity<CustomResponse> {
        val usuario = UsuarioMapper.buildUsuario(usuarioDto)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Perfil actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePerfil(usuario))
            )
        )
    }

}