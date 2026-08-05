package net.vplaygames.quizonconvertor.serializer

import kotlinx.serialization.json.Json
import net.vplaygames.quizonconvertor.model.QuizExport
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object JsonExporter {

    fun export(
        exports: List<QuizExport>,
        outputDir: File,
        prettyPrint: Boolean = true,
        warnings: List<String> = emptyList()
    ): List<File> {
        outputDir.mkdirs()

        val jsonSerializer = Json {
            this.prettyPrint = prettyPrint
            encodeDefaults = true
        }

        val generatedFiles = mutableListOf<File>()
        val reportLines = mutableListOf<String>()

        reportLines.add("=== QuizOnConvertor Conversion Report ===")
        reportLines.add("Generated At: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        reportLines.add("Output Directory: ${outputDir.absolutePath}")
        reportLines.add("Total Sections Converted: ${exports.size}\n")

        for (export in exports) {
            val rawName = export.paper.title
            val sanitizedName = sanitizeFileName(rawName)
            val jsonFile = File(outputDir, "$sanitizedName.json")

            val jsonText = jsonSerializer.encodeToString(QuizExport.serializer(), export)
            jsonFile.writeText(jsonText)
            generatedFiles.add(jsonFile)

            reportLines.add("--- Section: $rawName ---")
            reportLines.add("JSON File: ${jsonFile.name}")
            reportLines.add("Subject: ${export.subject.subject} (Code: ${export.subject.code}, Level: ${export.subject.level})")
            reportLines.add("Paper Title: ${export.paper.title} (Year: ${export.paper.year}, Term: ${export.paper.term}, Type: ${export.paper.examType})")
            reportLines.add("Total Questions: ${export.questions.size}")
            reportLines.add("  - MCQ: ${export.questions.count { it.qType == "mcq" }}")
            reportLines.add("  - MSQ: ${export.questions.count { it.qType == "msq" }}")
            reportLines.add("  - NAT: ${export.questions.count { it.qType == "nat" }}")

            val totalOptions = export.questions.flatMap { it.options }
            val correctOpts = totalOptions.count { it.isCorrect }
            val natAnswers = export.questions.count { it.correctAnswer != null }
            val questionImages = export.questions.count { it.image != null }
            val optionImages = totalOptions.count { it.image != null }
            val compPassages = export.comprehensions.size
            val compImages = export.comprehensions.count { it.image != null }

            reportLines.add("  - Correct Options Identified: $correctOpts")
            reportLines.add("  - Correct NAT Answers Identified: $natAnswers")
            reportLines.add("  - Question Diagrams Extracted: $questionImages")
            reportLines.add("  - Option Images Extracted: $optionImages")
            reportLines.add("  - Comprehension Passages: $compPassages")
            reportLines.add("  - Passage Images Extracted: $compImages")
            reportLines.add("")
        }

        if (warnings.isNotEmpty()) {
            reportLines.add("=== Warnings / Diagnostics ===")
            warnings.forEach { reportLines.add("- $it") }
            reportLines.add("")
        }

        val reportFile = File(outputDir, "conversion_report.txt")
        reportFile.writeText(reportLines.joinToString("\n"))
        generatedFiles.add(reportFile)

        return generatedFiles
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }
}
