package net.vplaygames.quizonconvertor.extractor

import org.apache.pdfbox.Loader
import java.io.File
import java.io.StringWriter

object PdfTextExtractor {
    fun extractText(pdfFile: File): List<PageContent> {
        require(pdfFile.exists()) { "PDF file does not exist: ${pdfFile.absolutePath}" }

        Loader.loadPDF(pdfFile).use { document ->
            val stripper = ColorTextStripper()
            stripper.writeText(document, StringWriter())
            return stripper.pagesContent
        }
    }
}
