package com.estonianport.centro_sis.common.emailService

import com.estonianport.centro_sis.common.errors.BusinessException
import com.estonianport.centro_sis.model.Usuario
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class EmailService {

    private val httpClient = HttpClient.newHttpClient()
    private val resendApiKey = System.getenv("MAIL_PASS")
        ?: throw IllegalStateException("MAIL_PASS no definido")
    private val fromEmail = "Centro-SIS <centro-sis@estonianport.com.ar>"

    fun isEmailValid(target: String): Boolean {
        return target.isNotEmpty() && EmailValidator.getInstance().isValid(target)
    }

    fun sendEmail(emailBody: Email) {
        if (!isEmailValid(emailBody.email)) {
            throw BusinessException("Email Invalido")
        }
        sendEmailTool(emailBody.content, emailBody.email, emailBody.subject)
    }

    private fun sendEmailTool(html: String, email: String, subject: String) {
        try {
            val body = """
            {
              "from": "$fromEmail",
              "to": ["$email"],
              "subject": "$subject",
              "html": ${jsonEscape(html)}
            }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer $resendApiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                println("Error Resend: ${response.body()}")
                throw BusinessException("No se pudo enviar el mail")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw BusinessException("No se pudo enviar el mail")
        }
    }

    fun loadHtmlTemplate(nombreArchivo: String): String {
        val inputStream = javaClass.classLoader
            .getResourceAsStream("templates/email/$nombreArchivo")
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

    fun enviarEmailAltaUsuario(usuario: Usuario, action: String, password: String) {

        if (!isEmailValid(usuario.email)) {
            throw BusinessException("Email Invalido")
        }

        val template = loadHtmlTemplate("alta_usuario.html")

        // ----------------- Content email -------------------------

        val content = renderTemplate(
            template,
            mapOf(
                "empresa_logo" to "https://iili.io/feEOZWg.png",
                "usuario" to usuario.email,
                "password" to password,
                "action" to action,
                "url_instagram" to "https://www.instagram.com/centro_cultural_tenri",
                "url_web" to "https://gakuseikai.tenri.com.ar",
                "url_wpp" to "https://api.whatsapp.com/send/?phone=%2B5491126885059&text=&type=phone_number&app_absent=0&wame_ctl=1",
                "imagen_ig" to "https://iili.io/3USINa4.png",
                "imagen_web" to "https://iili.io/3USIh6G.png",
                "imagen_wpp" to "https://iili.io/3USIk92.png",
            )
        )

        // ----------------- Envio email -------------------------

        sendEmail(
            Email(
                email = usuario.email,
                subject = action,
                content = content
            )
        )
    }

    private fun jsonEscape(html: String): String {
        return "\"" + html
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "") + "\""
    }
}