package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ExtractedImage
import net.vplaygames.quizonconvertor.extractor.PageContent
import net.vplaygames.quizonconvertor.extractor.PdfImageExtractor
import net.vplaygames.quizonconvertor.extractor.PdfTextExtractor
import net.vplaygames.quizonconvertor.model.QuizExport
import net.vplaygames.quizonconvertor.model.QuizPaperData
import net.vplaygames.quizonconvertor.model.SubjectData
import java.io.File

object PdfParser {

    fun parse(pdfFile: File, outputDir: File = File("output"), strict: Boolean = false): List<QuizExport> {
        val pages = PdfTextExtractor.extractText(pdfFile)
        val images = try {
            PdfImageExtractor.extractImages(pdfFile)
        } catch (e: Exception) {
            System.err.println("Warning: Could not extract images from PDF: ${e.message}")
            emptyList()
        }
        return parsePages(pages, images, outputDir, strict)
    }

    fun parsePages(
        pages: List<PageContent>,
        images: List<ExtractedImage> = emptyList(),
        outputDir: File = File("output"),
        strict: Boolean = false
    ): List<QuizExport> {
        val sectionContents = SectionSplitter.splitSections(pages)
        if (sectionContents.isEmpty()) {
            throw ConversionError("No sections found in PDF document.")
        }

        val lineClassifier = LineClassifier()
        val questionBuilder = QuestionBuilder()
        val exports = mutableListOf<QuizExport>()
        val errors = mutableListOf<String>()

        for ((sectionName, lines) in sectionContents) {
            val tokens = lineClassifier.classifyAll(lines)
            var questions = questionBuilder.buildQuestions(tokens)

            if (questions.isEmpty()) {
                val lastToken = tokens.lastOrNull()?.let { it::class.simpleName } ?: "None"
                val diagnostic = "Section \"$sectionName\" is incomplete: found 0 complete question blocks (last parsed token: $lastToken)."
                errors.add(diagnostic)
                if (strict) {
                    throw ConversionError(diagnostic)
                }
                continue
            }

            if (images.isNotEmpty()) {
                questions = ImageAssociator.associateAndSaveImages(
                    questions = questions,
                    tokens = tokens,
                    allImages = images,
                    outputDir = outputDir,
                    sectionName = sectionName
                )
            }

            val subject = extractSubjectData(sectionName, tokens)
            val paper = extractPaperData(sectionName)

            exports.add(
                QuizExport(
                    subject = subject,
                    paper = paper,
                    questions = questions,
                    tags = emptyList()
                )
            )
        }

        if (exports.isEmpty() && errors.isNotEmpty()) {
            throw ConversionError("Failed to extract any valid sections. Errors:\n" + errors.joinToString("\n"))
        }

        return exports
    }

    private fun extractSubjectData(sectionName: String, tokens: List<Token>): SubjectData {
        val titleToken = tokens.filterIsInstance<Token.SubjectTitle>().firstOrNull()
        val fullTitle = titleToken?.title ?: sectionName

        val level = when {
            fullTitle.contains("FOUNDATION", ignoreCase = true) -> "Foundation"
            fullTitle.contains("DIPLOMA", ignoreCase = true) -> "Diploma"
            fullTitle.contains("DEGREE", ignoreCase = true) -> "Degree"
            else -> "Foundation"
        }

        val cleanSubjectName = if (titleToken != null) {
            val parts = fullTitle.split(":")
            val namePart = parts.lastOrNull()?.trim() ?: fullTitle
            namePart.replace(Regex("""\s*\(.*?\)\s*"""), "").trim()
        } else {
            sectionName
        }

        return SubjectData(
            subject = cleanSubjectName,
            code = "",
            level = level,
            description = "Extracted from PDF section $sectionName",
            icon = ""
        )
    }

    private fun extractPaperData(sectionName: String): QuizPaperData {
        return QuizPaperData(
            title = sectionName,
            year = 2025,
            term = "may",
            examType = "endterm",
            totalDurationSeconds = 0,
            isPublished = false
        )
    }
}
