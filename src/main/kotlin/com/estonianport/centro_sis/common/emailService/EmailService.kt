package com.estonianport.centro_sis.common.emailService

import com.estonianport.centro_sis.common.errors.BusinessException
import com.estonianport.centro_sis.model.Usuario
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.mail.javamail.MimeMessageHelper


@Service
class EmailService {

    @Autowired
    lateinit var sender: JavaMailSender

    fun isEmailValid(target: String): Boolean {
        return target.isNotEmpty() && EmailValidator.getInstance().isValid(target)
    }

    fun sendEmail(emailBody: Email) {
        if(!isEmailValid(emailBody.email)){
            throw BusinessException("Email Invalido")
        }
        sendEmailTool(emailBody.content, emailBody.email, emailBody.subject)
    }

    private fun sendEmailTool(textMessage: String, email: String, subject: String) {
        val message: MimeMessage = sender.createMimeMessage()
        val helper = MimeMessageHelper(message)
        try {
            helper.setTo(email)
            helper.setText(textMessage, true)
            helper.setSubject(subject)
            sender.send(message)
        } catch (e: MessagingException) {
            throw BusinessException("No se pudo enviar el mail")
        }
    }

    fun loadHtmlTemplate(nombreArchivo: String): String {
        val inputStream = javaClass.classLoader.getResourceAsStream("templates/email/$nombreArchivo")
                ?: throw BusinessException("No se encontró la plantilla de mail")
        return inputStream.bufferedReader().use { it.readText() }
    }

    fun renderTemplate(template: String, replacements: Map<String, String>): String {
        var result = template
        for ((key, value) in replacements) {
            result = result.replace("{{${key}}}", value)
        }
        return result
    }

    fun enviarEmailAltaUsuario(usuario: Usuario, action: String, password : String) {

        if(!isEmailValid(usuario.email)){
            throw BusinessException("Email Invalido")
        }

        // ----------------- Armado email -------------------------
        val emailBody = Email(
            usuario.email,
            action
        )

        // ----------------- Content email -------------------------
        val template = loadHtmlTemplate("alta_usuario.html")
        emailBody.content = renderTemplate(template, mapOf(
            //EMPRESA LOGO DEBERIA APUNTAR A OTRO LADO, AHORA ESTA HARDCODEADO A UN LINK DE FACEBOOK
            "empresa_logo" to "https://iili.io/fku9DTF.png",
            "usuario" to usuario.email,
            "password" to password,
            "action" to action,
            "url_instagram" to "https://www.instagram.com/agendaza",
            "url_web" to "https://estonian-port.github.io/estonianport-landingpage/",
            "url_linkedin" to "https://www.linkedin.com/company/estonianport",
            "imagen_ig" to "https://iili.io/3USINa4.png",
            "imagen_web" to "https://iili.io/3USIh6G.png",
            "imagen_linkedin" to "https://iili.io/3USIwFf.png",
        ))

        // ----------------- Envio email -------------------------
        sendEmail(emailBody)

    }



}