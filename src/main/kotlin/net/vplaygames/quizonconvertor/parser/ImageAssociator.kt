package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ExtractedImage
import net.vplaygames.quizonconvertor.model.QuestionData
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

object ImageAssociator {

    fun associateAndSaveImages(
        questions: List<QuestionData>,
        tokens: List<Token>,
        allImages: List<ExtractedImage>,
        outputDir: File,
        sectionName: String
    ): List<QuestionData> {
        val sanitizedSection = sanitizeFileName(sectionName)
        val relativeImageDir = "images/$sanitizedSection"
        val targetDir = File(outputDir, relativeImageDir)
        if (allImages.any { !it.isUiIcon }) {
            targetDir.mkdirs()
        }

        val contentImages = allImages.filter { !it.isUiIcon }
        val iconImages = allImages.filter { it.isUiIcon }

        // Build question bounds map (questionOrder -> QuestionBound)
        val questionBounds = buildQuestionBounds(tokens, questions)

        val questionImageMap = mutableMapOf<Int, String>() // order -> relativePath
        val optionImageMap = mutableMapOf<Pair<Int, Int>, String>() // (order, serial) -> relativePath
        val optionCorrectnessFallback = mutableMapOf<Pair<Int, Int>, Boolean>() // (order, serial) -> isCorrect

        for (img in contentImages) {
            val bound = questionBounds.find { b ->
                img.pageNum in b.startPage..b.endPage &&
                    (img.pageNum > b.startPage || img.y >= b.startY - 10f) &&
                    (img.pageNum < b.endPage || b.endY == null || img.y <= b.endY + 10f)
            } ?: continue

            // Determine if option image or question image
            val matchingOptionToken = bound.optionTokens
                .filter { opt -> opt.line.pageNum == img.pageNum && opt.line.y <= img.y + 15f }
                .maxByOrNull { it.line.y }

            val fileName: String
            if (matchingOptionToken != null && matchingOptionToken.line.y <= img.y + 200f) {
                val serial = bound.getOptionSerial(matchingOptionToken.optionId)
                fileName = "q${bound.order}_opt${serial}.png"
                val relPath = "$relativeImageDir/$fileName"
                optionImageMap[Pair(bound.order, serial)] = relPath
            } else {
                fileName = "q${bound.order}_img.png"
                val relPath = "$relativeImageDir/$fileName"
                questionImageMap[bound.order] = relPath
            }

            val imageFile = File(targetDir, fileName)
            try {
                ImageIO.write(img.image, "png", imageFile)
            } catch (e: Exception) {
                System.err.println("Warning: Failed to save extracted image to ${imageFile.absolutePath}: ${e.message}")
            }
        }

        // Fallback for image-only options: use icon colorspace if present
        for (q in questions) {
            val bound = questionBounds.find { it.order == q.order } ?: continue
            for (opt in q.options) {
                if (opt.text.isBlank() && !opt.isCorrect) {
                    val optToken = bound.optionTokens.find { it.optionId == opt.sourceOptionId }
                    if (optToken != null) {
                        val nearbyIcon = iconImages.find { icon ->
                            icon.pageNum == optToken.line.pageNum && abs(icon.y - optToken.line.y) <= 15f
                        }
                        if (nearbyIcon != null && nearbyIcon.colorspace.contains("CalRGB", ignoreCase = true)) {
                            optionCorrectnessFallback[Pair(q.order, opt.serial)] = true
                        }
                    }
                }
            }
        }

        // Apply extracted images and correctness fallbacks to QuestionData
        return questions.map { q ->
            val qImg = questionImageMap[q.order] ?: q.image
            val updatedOptions = q.options.map { opt ->
                val optImg = optionImageMap[Pair(q.order, opt.serial)] ?: opt.image
                val fallbackCorrect = optionCorrectnessFallback[Pair(q.order, opt.serial)] ?: opt.isCorrect
                opt.copy(image = optImg, isCorrect = fallbackCorrect)
            }
            q.copy(image = qImg, options = updatedOptions)
        }
    }

    private data class QuestionBound(
        val order: Int,
        val startPage: Int,
        val startY: Float,
        val endPage: Int,
        val endY: Float?,
        val optionTokens: List<Token.OptionLine>
    ) {
        fun getOptionSerial(optionId: String): Int {
            val idx = optionTokens.indexOfFirst { it.optionId == optionId }
            return if (idx >= 0) idx + 1 else 1
        }
    }

    private fun buildQuestionBounds(tokens: List<Token>, questions: List<QuestionData>): List<QuestionBound> {
        val qHeaderTokens = tokens.filterIsInstance<Token.QuestionHeader>()
        val bounds = mutableListOf<QuestionBound>()

        for (i in qHeaderTokens.indices) {
            val currentHeader = qHeaderTokens[i]
            val nextHeader = qHeaderTokens.getOrNull(i + 1)
            val matchingQ = questions.find { it.sourceQuestionNumber == currentHeader.number } ?: continue

            val startPage = currentHeader.line.pageNum
            val startY = currentHeader.line.y

            val endPage = nextHeader?.line?.pageNum ?: (startPage + 2)
            val endY = nextHeader?.line?.y

            val qTokens = tokens.subList(
                tokens.indexOf(currentHeader),
                if (nextHeader != null) tokens.indexOf(nextHeader) else tokens.size
            )
            val optionTokens = qTokens.filterIsInstance<Token.OptionLine>()

            bounds.add(
                QuestionBound(
                    order = matchingQ.order,
                    startPage = startPage,
                    startY = startY,
                    endPage = endPage,
                    endY = endY,
                    optionTokens = optionTokens
                )
            )
        }
        return bounds
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }
}
