package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.repository.UsuarioRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.EncodeHintType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class QrService(
    private val usuarioRepository: UsuarioRepository
) {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    /**
     * Genera un ZIP con todos los QR de usuarios ACTIVOS
     * Nombre de archivo: DNI_Nombre_Apellido.png
     */

    fun generarZipConTodosLosQr(): ByteArray {
        val usuarios = usuarioRepository.findAll()
            .filter { it.estado == EstadoType.ACTIVO }

        val outputStream = ByteArrayOutputStream()
        val zipOutputStream = ZipOutputStream(outputStream)

        usuarios.forEach { usuario ->
            try {
                // MISMO FORMATO QUE EL FRONTEND
                val qrData = mapOf(
                    "usuarioId" to usuario.id,
                    "tipo" to "PERMANENTE"
                )

                // Serializar con Jackson (mismo que React Native usa por defecto)
                val qrContent = objectMapper.writeValueAsString(qrData)

                val qrBytes = generarQrPng(qrContent, size = 512)

                // Nombre: DNI_Nombre_Apellido.png
                val fileName = sanitizarNombreArchivo(
                    "${usuario.dni}_${usuario.nombre}_${usuario.apellido}.png"
                )

                val zipEntry = ZipEntry(fileName)
                zipOutputStream.putNextEntry(zipEntry)
                zipOutputStream.write(qrBytes)
                zipOutputStream.closeEntry()

            } catch (e: Exception) {
                println("❌ Error generando QR para usuario ${usuario.id}: ${e.message}")
                e.printStackTrace()
            }
        }

        zipOutputStream.close()
        return outputStream.toByteArray()
    }

    /**
     * GENERAR QR PNG - Configuración idéntica a react-native-qrcode-svg
     */
    private fun generarQrPng(contenido: String, size: Int = 512): ByteArray {
        val qrCodeWriter = QRCodeWriter()

        // Configuración para compatibilidad máxima
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0  // Sin margen, igual que react-native-qrcode-svg
        )

        val bitMatrix = qrCodeWriter.encode(
            contenido,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Normalizar nombre de archivo (quitar acentos, espacios, etc.)
     */
    private fun sanitizarNombreArchivo(nombre: String): String {
        return nombre
            .replace(" ", "_")
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
            .replace("Á", "A").replace("É", "E").replace("Í", "I")
            .replace("Ó", "O").replace("Ú", "U")
            .replace("ñ", "n").replace("Ñ", "N")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")  // Solo alfanuméricos
    }
}