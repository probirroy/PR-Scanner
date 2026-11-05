package com.example.prscanner.engine

import android.content.Context
import android.graphics.Bitmap
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.model.DocumentPageEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Native document converter engine that generates valid Microsoft Word (.docx),
 * Excel (.csv / spreadsheet tables), PowerPoint (.pptx slide outline), and text reports
 * with zero external dependencies.
 */
object DocxEngine {

    /**
     * Converts document text and structure into a genuine Microsoft Word (.docx) file.
     * Uses standard OpenXML packaging format.
     */
    fun createDocxFile(
        outputFile: File,
        title: String,
        pagesText: List<String>
    ): Boolean {
        try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // 1. [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
                zos.write(contentTypes.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // 2. _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
                zos.write(rels.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // 3. word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                val sb = java.lang.StringBuilder()
                sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
                sb.append("""<w:body>""")

                // Document Title
                val escapedTitle = escapeXml(title)
                sb.append("<w:p>")
                sb.append("<w:pPr><w:pStyle w:val=\"Heading1\"/><w:jc w:val=\"center\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"36\"/><w:color w:val=\"1E3A8A\"/></w:rPr>")
                sb.append("<w:t>").append(escapedTitle).append("</w:t></w:r>")
                sb.append("</w:p>")

                // Metadata subtitle
                sb.append("<w:p>")
                sb.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:i/><w:sz w:val=\"20\"/><w:color w:val=\"64748B\"/></w:rPr>")
                sb.append("<w:t>Converted with PR Scanner Document Studio</w:t></w:r>")
                sb.append("</w:p>")

                // Divider
                sb.append("<w:p/>")

                // Page contents
                pagesText.forEachIndexed { index, text ->
                    sb.append("<w:p>")
                    sb.append("<w:pPr><w:pStyle w:val=\"Heading2\"/></w:pPr>")
                    sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"26\"/><w:color w:val=\"2563EB\"/></w:rPr>")
                    sb.append("<w:t>Page ").append(index + 1).append("</w:t></w:r>")
                    sb.append("</w:p>")

                    val lines = text.split("\n")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            sb.append("<w:p>")
                            sb.append("<w:r><w:rPr><w:sz w:val=\"22\"/></w:rPr>")
                            sb.append("<w:t>").append(escapeXml(trimmed)).append("</w:t></w:r>")
                            sb.append("</w:p>")
                        }
                    }

                    if (index < pagesText.size - 1) {
                        // Page break
                        sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                    }
                }

                sb.append("</w:body></w:document>")
                zos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Converts OCR text or tabular content into an Excel CSV spreadsheet (.csv).
     */
    fun createCsvSpreadsheet(
        outputFile: File,
        pagesText: List<String>
    ): Boolean {
        try {
            OutputStreamWriter(FileOutputStream(outputFile), StandardCharsets.UTF_8).use { writer ->
                // Header
                writer.write("Page,Line,Extracted Data,Column 2,Column 3\n")
                pagesText.forEachIndexed { pageIdx, text ->
                    val lines = text.split("\n")
                    lines.forEachIndexed { lineIdx, line ->
                        val clean = line.replace("\"", "\"\"").trim()
                        if (clean.isNotEmpty()) {
                            // If contains tabs or multiple spaces, separate into columns
                            val parts = clean.split(Regex("[\\t|;,]+")).map { it.trim().replace("\"", "\"\"") }
                            if (parts.size > 1) {
                                writer.write("${pageIdx + 1},${lineIdx + 1},\"${parts.joinToString("\",\"")}\"\n")
                            } else {
                                writer.write("${pageIdx + 1},${lineIdx + 1},\"$clean\",,\n")
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Creates a PowerPoint slide outline document (.pptx text structure).
     */
    fun createSlideDeckText(
        outputFile: File,
        title: String,
        pagesText: List<String>
    ): Boolean {
        try {
            OutputStreamWriter(FileOutputStream(outputFile), StandardCharsets.UTF_8).use { writer ->
                writer.write("=========================================\n")
                writer.write("PRESENTATION SLIDE DECK: $title\n")
                writer.write("Generated by PR Scanner Slide Studio\n")
                writer.write("=========================================\n\n")

                pagesText.forEachIndexed { index, text ->
                    writer.write("--- SLIDE ${index + 1} ---\n")
                    val lines = text.split("\n").filter { it.isNotBlank() }
                    val slideTitle = lines.firstOrNull() ?: "Slide ${index + 1}"
                    writer.write("TITLE: $slideTitle\n\n")
                    writer.write("BULLET POINTS:\n")
                    lines.drop(1).take(8).forEach { bullet ->
                        writer.write("  • ${bullet.trim()}\n")
                    }
                    writer.write("\n")
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
