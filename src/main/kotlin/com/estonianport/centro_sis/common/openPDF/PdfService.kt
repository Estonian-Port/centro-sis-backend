package com.estonianport.centro_sis.common.openPDF

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfContentByte
import org.springframework.stereotype.Service
import java.awt.Color
import java.net.URL


@Service
class PdfService {

    private fun setPageSize(document: Document) {
        document.setPageSize(PageSize.A4)
    }

    private fun setLogo(document: Document, logoUrl: String) {
        val logo = Image.getInstance(URL(logoUrl))

        logo.scaleToFit(100f, 100f) // Ajusta el tamaño del logo
        logo.alignment = Element.ALIGN_CENTER
        document.add(logo)
    }

    private fun setHeader(document: Document, fontNormal : Font) {

        val header = Paragraph()


        document.add(header)
    }



    private fun setCuadroDescripcion(document : Document, cb: PdfContentByte, cuadroGrande : Boolean) {

        // Setea color y ancho del cuadro
        cb.setColorStroke(Color.DARK_GRAY)
        cb.setLineWidth(0.5f)

        // Cambia la medida del cuadro de descripcion en base al tipo de comprobante
        var cuadroAltura = -140f

        if(cuadroGrande){
            cuadroAltura = -400f

            // Coloca una linea divisoria correspondiente al cuadro grande
            cb.moveTo(30f, 455f) // Mueve a la posición donde comienza la línea
            cb.lineTo(560f, 455f) // Dibuja la línea hasta el otro lado

            // Coloca una linea divisoria correspondiente al cuadro grande
            cb.moveTo(30f, 415f) // Mueve a la posición donde comienza la línea
            cb.lineTo(560f, 415f) // Dibuja la línea hasta el otro lado
            cb.stroke()
        }

        // Dibuja el cuadro de descripcion
        cb.rectangle(30f, 565f, 530f, cuadroAltura)
        cb.stroke()

        // Dibuja la línea divisioria de descripcion
        cb.moveTo(30f, 525f) // Mueve a la posición donde comienza la línea
        cb.lineTo(560f, 525f) // Dibuja la línea hasta el otro lado
        cb.stroke()
    }

    private fun setMargenes(document : Document, cb: PdfContentByte) {

        // Obtén las dimensiones de la página
        val pageSize = document.pageSize

        // Setea grosor y color
        cb.setLineWidth(2f)
        cb.setColorStroke(Color.BLACK)

        // Dibuja margenes
        cb.rectangle(10f, 10f, pageSize.width - 20f, pageSize.height - 20f) // Coordenadas 0, 0 y dimensiones de la página
        cb.stroke()
    }



}