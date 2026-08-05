package net.vplaygames.quizonconvertor

import net.vplaygames.quizonconvertor.extractor.PdfTextExtractor
import net.vplaygames.quizonconvertor.extractor.TextColor
import java.io.File

fun main(args: Array<String>) {
    val pdfPath = args.firstOrNull() ?: "Sem1 Maths1.pdf"
    val pdfFile = File(pdfPath)

    if (!pdfFile.exists()) {
        println("Error: File not found at $pdfPath")
        return
    }

    println("Extracting color-annotated text from: ${pdfFile.absolutePath}")
    val pages = PdfTextExtractor.extractText(pdfFile)

    var totalLines = 0
    var greenLines = 0
    var redLines = 0
    var blackLines = 0

    pages.forEach { page ->
        println("--- Page ${page.pageNumber} (${page.lines.size} lines) ---")
        page.lines.forEach { line ->
            totalLines++
            when (line.color) {
                TextColor.GREEN -> greenLines++
                TextColor.RED -> redLines++
                TextColor.BLACK -> blackLines++
                else -> {}
            }
            val tag = "[${line.color.name.padEnd(5)}]"
            println("$tag ${line.text}")
        }
    }

    println("\n=== Extraction Summary ===")
    println("Total Pages: ${pages.size}")
    println("Total Lines: $totalLines")
    println("Black Lines: $blackLines")
    println("Green Lines: $greenLines")
    println("Red Lines:   $redLines")
}
