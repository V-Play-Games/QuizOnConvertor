package net.vplaygames.quizonconvertor

import net.vplaygames.quizonconvertor.parser.ConversionError
import net.vplaygames.quizonconvertor.parser.PdfParser
import java.io.File

fun main(args: Array<String>) {
    val pdfPath = args.firstOrNull() ?: "Sem1 Maths1.pdf"
    val pdfFile = File(pdfPath)

    if (!pdfFile.exists()) {
        println("Error: File not found at $pdfPath")
        return
    }

    val outputDir = File("output")
    println("Parsing PDF into structured data: ${pdfFile.absolutePath}")

    try {
        val exports = PdfParser.parse(pdfFile, outputDir = outputDir, strict = false)

        println("\n=== Parsing & Image Pipeline Summary ===")
        println("Total Sections Extracted: ${exports.size}")

        exports.forEachIndexed { idx, export ->
            println("\n--- Section ${idx + 1}: ${export.paper.title} ---")
            println("Subject: ${export.subject.subject} (Level: ${export.subject.level})")
            println("Total Questions: ${export.questions.size}")

            val mcqCount = export.questions.count { it.qType == "mcq" }
            val msqCount = export.questions.count { it.qType == "msq" }
            val natCount = export.questions.count { it.qType == "nat" }

            println("  - MCQ Questions: $mcqCount")
            println("  - MSQ Questions: $msqCount")
            println("  - NAT Questions: $natCount")

            val optionsWithAnswerKey = export.questions.flatMap { it.options }.count { it.isCorrect }
            val natWithAnswerKey = export.questions.count { it.correctAnswer != null }
            println("  - Correct Options Identified: $optionsWithAnswerKey")
            println("  - Correct NAT Answers Identified: $natWithAnswerKey")

            val questionImages = export.questions.count { it.image != null }
            val optionImages = export.questions.flatMap { it.options }.count { it.image != null }
            println("  - Question Diagrams Extracted: $questionImages")
            println("  - Option Images Extracted: $optionImages")
        }
    } catch (e: ConversionError) {
        println("Conversion Error: ${e.message}")
    } catch (e: Exception) {
        println("Unexpected Error during parsing: ${e.message}")
        e.printStackTrace()
    }
}


