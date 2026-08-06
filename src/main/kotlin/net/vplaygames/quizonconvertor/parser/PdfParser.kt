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

    fun parse(
        pdfFile: File,
        outputDir: File = File("output"),
        strict: Boolean = false,
        progressCallback: ((step: String, detail: String, percent: Int) -> Unit)? = null
    ): List<QuizExport> {
        progressCallback?.invoke("Extracting text", "Reading PDF document...", 5)
        val pages = PdfTextExtractor.extractText(pdfFile)
        progressCallback?.invoke("Extracted text", "${pages.size} page(s) read", 20)

        progressCallback?.invoke("Extracting images", "Searching for embedded diagrams...", 25)
        val images = try {
            PdfImageExtractor.extractImages(pdfFile)
        } catch (e: Exception) {
            System.err.println("Warning: Could not extract images from PDF: ${e.message}")
            emptyList()
        }
        progressCallback?.invoke("Extracted images", "${images.size} image(s) found", 35)

        return parsePages(pages, images, outputDir, strict, progressCallback)
    }

    fun parsePages(
        pages: List<PageContent>,
        images: List<ExtractedImage> = emptyList(),
        outputDir: File = File("output"),
        strict: Boolean = false,
        progressCallback: ((step: String, detail: String, percent: Int) -> Unit)? = null
    ): List<QuizExport> {
        progressCallback?.invoke("Splitting sections", "Grouping page contents by section...", 40)
        val sectionContents = SectionSplitter.splitSections(pages)
        if (sectionContents.isEmpty()) {
            throw ConversionError("No sections found in PDF document.")
        }

        val lineClassifier = LineClassifier()
        val questionBuilder = QuestionBuilder()
        val exports = mutableListOf<QuizExport>()
        val errors = mutableListOf<String>()

        val totalSections = sectionContents.size
        sectionContents.forEachIndexed { index, (sectionName, lines) ->
            val sectionPercent = 45 + ((index + 1) * 40 / totalSections)
            progressCallback?.invoke("Parsing section", "\"$sectionName\" (${index + 1}/$totalSections)", sectionPercent)

            val tokens = lineClassifier.classifyAll(lines)
            val buildResult = questionBuilder.buildAll(tokens)
            var questions = buildResult.questions
            var comprehensions = buildResult.comprehensions

            if (questions.isEmpty()) {
                val lastToken = tokens.lastOrNull()?.let { it::class.simpleName } ?: "None"
                val diagnostic = "Section \"$sectionName\" is incomplete: found 0 complete question blocks (last parsed token: $lastToken)."
                errors.add(diagnostic)
                if (strict) {
                    throw ConversionError(diagnostic)
                }
                return@forEachIndexed
            }

            if (images.isNotEmpty()) {
                val assocResult = ImageAssociator.associateAndSaveImages(
                    questions = questions,
                    comprehensions = comprehensions,
                    tokens = tokens,
                    allImages = images,
                    outputDir = outputDir,
                    sectionName = sectionName
                )
                questions = assocResult.questions
                comprehensions = assocResult.comprehensions
            }

            val subject = extractSubjectData(sectionName, tokens)
            val paper = extractPaperData(sectionName)

            exports.add(
                QuizExport(
                    subject = subject,
                    paper = paper,
                    questions = questions,
                    comprehensions = comprehensions,
                    tags = emptyList()
                )
            )
        }

        if (exports.isEmpty() && errors.isNotEmpty()) {
            throw ConversionError("Failed to extract any valid sections. Errors:\n" + errors.joinToString("\n"))
        }

        progressCallback?.invoke("Finalizing parse", "Extracted ${exports.size} valid section(s)", 88)
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
