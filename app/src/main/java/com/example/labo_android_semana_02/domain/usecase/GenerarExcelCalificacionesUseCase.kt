package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera un libro de Excel nativo (.xlsx - Office Open XML) con la tabla de calificaciones
 * de cada integrante del curso, el detalle por actividad y los totales en escala 0.0-5.0 y 0-100.
 */
class GenerarExcelCalificacionesUseCase {

    operator fun invoke(
        cursoNombre: String,
        instructorNombre: String,
        actividades: List<Actividad>,
        calificaciones: List<ResumenCalificacionEstudiante>,
    ): ByteArray {
        val actividadesOrdenadas = if (actividades.isNotEmpty()) {
            actividades.map { ActCol(it.id, it.titulo, it.puntajeMaximo) }
        } else {
            calificaciones.firstOrNull()?.detalle?.map {
                ActCol(it.actividadId, it.titulo, it.puntajeMaximo)
            }.orEmpty()
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            escribirEntrada(zip, "[Content_Types].xml", contentTypesXml())
            escribirEntrada(zip, "_rels/.rels", rootRelsXml())
            escribirEntrada(zip, "xl/workbook.xml", workbookXml())
            escribirEntrada(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            escribirEntrada(zip, "xl/styles.xml", stylesXml())
            escribirEntrada(
                zip,
                "xl/worksheets/sheet1.xml",
                sheetXml(
                    cursoNombre = cursoNombre,
                    instructorNombre = instructorNombre,
                    actividades = actividadesOrdenadas,
                    calificaciones = calificaciones,
                ),
            )
        }
        return baos.toByteArray()
    }

    private data class ActCol(
        val id: Int,
        val titulo: String,
        val puntajeMaximo: Double,
    )

    private fun escribirEntrada(zip: ZipOutputStream, nombre: String, contenido: String) {
        val entry = ZipEntry(nombre)
        zip.putNextEntry(entry)
        zip.write(contenido.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="Calificaciones" sheetId="1" r:id="rId1"/>
            </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    /**
     * Estilos (cellXfs):
     * 0 = Normal
     * 1 = Título principal (Negrita 13pt, color #4A413C)
     * 2 = Encabezado de tabla (Fondo terracota #BB6C43, texto blanco en negrita, borde)
     * 3 = Celda de datos texto con borde
     * 4 = Celda numérica decimal (0.00) con borde
     * 5 = Celda de total destacada (Fondo #F7DFD0, negrita, formato 0.00)
     */
    private fun stylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <numFmts count="1">
                <numFmt numFmtId="164" formatCode="0.00"/>
            </numFmts>
            <fonts count="4">
                <font><sz val="11"/><color rgb="FF26201D"/><name val="Calibri"/></font>
                <font><b/><sz val="13"/><color rgb="FF4A413C"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FF4A413C"/><name val="Calibri"/></font>
            </fonts>
            <fills count="4">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FFBB6C43"/><bgColor indexed="64"/></patternFill></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FFF7DFD0"/><bgColor indexed="64"/></patternFill></fill>
            </fills>
            <borders count="2">
                <border><left/><right/><top/><bottom/><diagonal/></border>
                <border>
                    <left style="thin"><color rgb="FFCCB499"/></left>
                    <right style="thin"><color rgb="FFCCB499"/></right>
                    <top style="thin"><color rgb="FFCCB499"/></top>
                    <bottom style="thin"><color rgb="FFCCB499"/></bottom>
                    <diagonal/>
                </border>
            </borders>
            <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
            </cellStyleXfs>
            <cellXfs count="6">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
                <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
                <xf numFmtId="164" fontId="3" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
            </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun sheetXml(
        cursoNombre: String,
        instructorNombre: String,
        actividades: List<ActCol>,
        calificaciones: List<ResumenCalificacionEstudiante>,
    ): String {
        val totalColumnas = 3 + actividades.size + 3
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Ancho de columnas para lectura cómoda en Excel
        sb.append("<cols>")
        sb.append("""<col min="1" max="1" width="6" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="28" customWidth="1"/>""")
        sb.append("""<col min="3" max="3" width="30" customWidth="1"/>""")
        if (actividades.isNotEmpty()) {
            sb.append("""<col min="4" max="${3 + actividades.size}" width="20" customWidth="1"/>""")
        }
        val colCalif = 4 + actividades.size
        sb.append("""<col min="$colCalif" max="$colCalif" width="16" customWidth="1"/>""")
        sb.append("""<col min="${colCalif + 1}" max="${colCalif + 2}" width="18" customWidth="1"/>""")
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Fila 1: Título del curso
        sb.append("""<row r="1">""")
        sb.append(celdaTexto(1, 1, "Curso: $cursoNombre", style = 1))
        sb.append("</row>")

        // Fila 2: Instructor
        sb.append("""<row r="2">""")
        sb.append(celdaTexto(2, 1, "Instructor: $instructorNombre", style = 0))
        sb.append("</row>")

        // Fila 4: Encabezados de la tabla
        val filaHeader = 4
        sb.append("""<row r="$filaHeader">""")
        var col = 1
        sb.append(celdaTexto(filaHeader, col++, "N°", style = 2))
        sb.append(celdaTexto(filaHeader, col++, "Aprendiz", style = 2))
        sb.append(celdaTexto(filaHeader, col++, "Correo electrónico", style = 2))
        actividades.forEach { act ->
            val maxFmt = if (act.puntajeMaximo >= 100.0) "100" else "5.0"
            sb.append(celdaTexto(filaHeader, col++, "${act.titulo} (/$maxFmt)", style = 2))
        }
        sb.append(celdaTexto(filaHeader, col++, "Calificadas", style = 2))
        sb.append(celdaTexto(filaHeader, col++, "Total (0.0 - 5.0)", style = 2))
        sb.append(celdaTexto(filaHeader, col, "Total (0 - 100)", style = 2))
        sb.append("</row>")

        // Filas de aprendices
        var filaActual = 5
        calificaciones.forEachIndexed { idx, res ->
            sb.append("""<row r="$filaActual">""")
            var cIdx = 1
            sb.append(celdaNumero(filaActual, cIdx++, (idx + 1).toDouble(), style = 3))
            sb.append(celdaTexto(filaActual, cIdx++, res.aprendiz?.nombre ?: "Aprendiz ${idx + 1}", style = 3))
            sb.append(celdaTexto(filaActual, cIdx++, res.aprendiz?.email ?: "", style = 3))

            val mapaDetalle = res.detalle.associateBy { it.actividadId }
            actividades.forEach { act ->
                val det = mapaDetalle[act.id]
                when {
                    det == null -> sb.append(celdaNumero(filaActual, cIdx++, 0.0, style = 4))
                    det.exenta -> sb.append(celdaTexto(filaActual, cIdx++, "EXENTA", style = 3))
                    det.nota != null -> sb.append(celdaNumero(filaActual, cIdx++, det.nota, style = 4))
                    else -> sb.append(celdaNumero(filaActual, cIdx++, 0.0, style = 4))
                }
            }

            sb.append(
                celdaTexto(
                    filaActual,
                    cIdx++,
                    "${res.actividadesCalificadas}/${res.actividadesEvaluables}",
                    style = 3,
                ),
            )
            sb.append(celdaNumero(filaActual, cIdx++, res.nota05, style = 5))
            sb.append(celdaNumero(filaActual, cIdx, res.nota0100, style = 5))
            sb.append("</row>")
            filaActual++
        }

        // Fila de promedio grupal si hay aprendices
        if (calificaciones.isNotEmpty()) {
            val prom05 = calificaciones.map { it.nota05 }.average()
            val prom100 = calificaciones.map { it.nota0100 }.average()
            sb.append("""<row r="$filaActual">""")
            sb.append(celdaTexto(filaActual, 1, "", style = 5))
            sb.append(celdaTexto(filaActual, 2, "PROMEDIO DEL GRUPO", style = 5))
            for (rellenoCol in 3..(totalColumnas - 2)) {
                sb.append(celdaTexto(filaActual, rellenoCol, "", style = 5))
            }
            sb.append(celdaNumero(filaActual, totalColumnas - 1, prom05, style = 5))
            sb.append(celdaNumero(filaActual, totalColumnas, prom100, style = 5))
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun celdaTexto(fila: Int, col: Int, valor: String, style: Int): String {
        val ref = "${nombreColumnaExcel(col)}$fila"
        val escapado = escaparXml(valor)
        return """<c r="$ref" s="$style" t="inlineStr"><is><t>$escapado</t></is></c>"""
    }

    private fun celdaNumero(fila: Int, col: Int, valor: Double, style: Int): String {
        val ref = "${nombreColumnaExcel(col)}$fila"
        val numStr = if (valor % 1.0 == 0.0 && style == 3) {
            valor.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", valor)
        }
        return """<c r="$ref" s="$style"><v>$numStr</v></c>"""
    }

    private fun nombreColumnaExcel(col1Based: Int): String {
        var n = col1Based
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escaparXml(texto: String): String = buildString(texto.length) {
        for (ch in texto) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
