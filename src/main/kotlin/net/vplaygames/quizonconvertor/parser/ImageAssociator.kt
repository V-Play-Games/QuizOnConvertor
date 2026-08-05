package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ExtractedImage
import net.vplaygames.quizonconvertor.model.ComprehensionData
import net.vplaygames.quizonconvertor.model.QuestionData
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

data class ImageAssociationResult(
    val questions: List<QuestionData>,
    val comprehensions: List<ComprehensionData>
)

object ImageAssociator {

    fun associateAndSaveImages(
        questions: List<QuestionData>,
        tokens: List<Token>,
        allImages: List<ExtractedImage>,
        outputDir: File,
        sectionName: String
    ): List<QuestionData> {
        return associateAndSaveImages(
            questions = questions,
            comprehensions = emptyList(),
            tokens = tokens,
            allImages = allImages,
            outputDir = outputDir,
            sectionName = sectionName
        ).questions
    }

    fun associateAndSaveImages(
        questions: List<QuestionData>,
        comprehensions: List<ComprehensionData>,
        tokens: List<Token>,
        allImages: List<ExtractedImage>,
        outputDir: File,
        sectionName: String
    ): ImageAssociationResult {
        val sanitizedSection = sanitizeFileName(sectionName)
        val relativeImageDir = "images/$sanitizedSection"
        val targetDir = File(outputDir, relativeImageDir)
        if (allImages.any { !it.isUiIcon }) {
            targetDir.mkdirs()
        }

        val contentImages = allImages.filter { !it.isUiIcon }
        val iconImages = allImages.filter { it.isUiIcon }

        // Build question and comprehension bounds maps
        val questionBounds = buildQuestionBounds(tokens, questions)
        val comprehensionBounds = buildComprehensionBounds(tokens, comprehensions)

        val questionImageMap = mutableMapOf<Int, String>() // order -> relativePath
        val optionImageMap = mutableMapOf<Pair<Int, Int>, String>() // (order, serial) -> relativePath
        val comprehensionImageMap = mutableMapOf<String, String>() // sourceId -> relativePath
        val optionCorrectnessFallback = mutableMapOf<Pair<Int, Int>, Boolean>() // (order, serial) -> isCorrect

        for (img in contentImages) {
            // Check if image belongs to a comprehension passage block
            val compBound = comprehensionBounds.find { b ->
                img.pageNum in b.startPage..b.endPage &&
                    (img.pageNum > b.startPage || img.y >= b.startY - 10f) &&
                    (img.pageNum < b.endPage || b.endY == null || img.y <= b.endY + 10f)
            }

            if (compBound != null) {
                val fileName = "comp_${sanitizeFileName(compBound.sourceId)}_img.png"
                val relPath = "$relativeImageDir/$fileName"
                comprehensionImageMap[compBound.sourceId] = relPath
                val imageFile = File(targetDir, fileName)
                try {
                    ImageIO.write(img.image, "png", imageFile)
                } catch (e: Exception) {
                    System.err.println("Warning: Failed to save extracted comprehension image to ${imageFile.absolutePath}: ${e.message}")
                }
                continue
            }

            val bound = questionBounds.find { b ->
                img.pageNum in b.startPage..b.endPage &&
                    (img.pageNum > b.startPage || img.y >= b.startY - 10f) &&
                    (img.pageNum < b.endPage || b.endY == null || img.y <= b.endY + 10f)
            } ?: continue

            // Determine if this is a question image or an option image.
            //
            // Rule: images appearing before the "Options :" header belong to the question;
            // images after it belong to an option.
            //
            // Option images are rendered ABOVE their option text line in this PDF format
            // (the image is embedded as a figure before the label text). We find the closest
            // option token whose line appears just BELOW the image (y_token >= y_img).
            //
            // Cross-page: if an image is the first thing on a new page and no option token
            // sits below it on that page, fall back to checking whether the last option on
            // the previous page has no image yet (it wraps its content onto the next page).

            val imageIsBeforeOptions = bound.optionsHeaderPage == null ||
                img.pageNum < bound.optionsHeaderPage!! ||
                (img.pageNum == bound.optionsHeaderPage && img.y < bound.optionsHeaderY!!)

            val fileName: String
            if (imageIsBeforeOptions) {
                // Image is part of the question stem.
                fileName = "q${bound.order}_img.png"
                val relPath = "$relativeImageDir/$fileName"
                questionImageMap[bound.order] = relPath
            } else {
                // Image is in the options section; find the closest unmatched option token just below it.
                //
                // An option's OWN image sits immediately before its label: the gap between
                // image.y and label.y is approximately equal to the image's display height.
                // A cross-page continuation image (e.g. option 116 whose text wraps from p3 to
                // the top of p4) has a much larger gap to the next option label on the new page.
                //
                // We use img.height + 20f as the tolerance, which fits the direct case
                // (gap ≈ displayHeight) while excluding cross-page continuations (gap >> displayHeight).
                val samePage = bound.optionTokens
                    .filter { opt ->
                        opt.line.pageNum == img.pageNum &&
                            opt.line.y >= img.y &&
                            opt.line.y <= img.y + img.height + 20f &&
                            !optionImageMap.containsKey(Pair(bound.order, bound.getOptionSerial(opt.optionId)))
                    }
                    .minByOrNull { it.line.y }

                val matchingOptionToken = samePage
                    ?: bound.optionTokens   // cross-page fallback
                        .filter { opt ->
                            opt.line.pageNum == img.pageNum - 1 &&
                                !optionImageMap.containsKey(Pair(bound.order, bound.getOptionSerial(opt.optionId)))
                        }
                        .maxByOrNull { it.line.y }  // last unmatched option on the previous page

                if (matchingOptionToken != null) {
                    val serial = bound.getOptionSerial(matchingOptionToken.optionId)
                    fileName = "q${bound.order}_opt${serial}.png"
                    val relPath = "$relativeImageDir/$fileName"
                    optionImageMap[Pair(bound.order, serial)] = relPath
                } else {
                    // Still no match; treat as extra question image (shouldn't normally happen).
                    fileName = "q${bound.order}_img.png"
                    val relPath = "$relativeImageDir/$fileName"
                    questionImageMap[bound.order] = relPath
                }
            }

            val imageFile = File(targetDir, fileName)
            try {
                ImageIO.write(img.image, "png", imageFile)
            } catch (e: Exception) {
                System.err.println("Warning: Failed to save extracted image to ${imageFile.absolutePath}: ${e.message}")
            }
        }

        // Fallback for image-only options: check the nearby icon's pixel content to determine
        // correctness. A "correct" icon is visually green/filled; an incorrect one is empty/grey.
        // We look for a small icon near the option line and sample its pixels — if the icon
        // contains a noticeably green pixel, the option is correct.
        for (q in questions) {
            val bound = questionBounds.find { it.order == q.order } ?: continue
            for (opt in q.options) {
                if (opt.text.isBlank() && !opt.isCorrect) {
                    val optToken = bound.optionTokens.find { it.optionId == opt.sourceOptionId }
                    if (optToken != null) {
                        val nearbyIcon = iconImages.find { icon ->
                            icon.pageNum == optToken.line.pageNum && abs(icon.y - optToken.line.y) <= 15f
                        }
                        if (nearbyIcon != null && iconIsGreen(nearbyIcon.image)) {
                            optionCorrectnessFallback[Pair(q.order, opt.serial)] = true
                        }
                    }
                }
            }
        }

        // Apply extracted images and correctness fallbacks to QuestionData
        val updatedQuestions = questions.map { q ->
            val qImg = questionImageMap[q.order] ?: q.image
            val updatedOptions = q.options.map { opt ->
                val optImg = optionImageMap[Pair(q.order, opt.serial)] ?: opt.image
                val fallbackCorrect = optionCorrectnessFallback[Pair(q.order, opt.serial)] ?: opt.isCorrect
                opt.copy(image = optImg, isCorrect = fallbackCorrect)
            }
            q.copy(image = qImg, options = updatedOptions)
        }

        val updatedComprehensions = comprehensions.map { c ->
            val cImg = comprehensionImageMap[c.sourceId] ?: c.image
            c.copy(image = cImg)
        }

        return ImageAssociationResult(
            questions = updatedQuestions,
            comprehensions = updatedComprehensions
        )
    }

    private data class ComprehensionBound(
        val sourceId: String,
        val startPage: Int,
        val startY: Float,
        val endPage: Int,
        val endY: Float?
    )

    private fun buildComprehensionBounds(tokens: List<Token>, comprehensions: List<ComprehensionData>): List<ComprehensionBound> {
        val compHeaderTokens = tokens.filterIsInstance<Token.ComprehensionHeader>()
        val bounds = mutableListOf<ComprehensionBound>()

        for (i in compHeaderTokens.indices) {
            val currentHeader = compHeaderTokens[i]
            val matchingC = comprehensions.find { it.sourceId == currentHeader.id } ?: continue

            val headerIndex = tokens.indexOf(currentHeader)
            val subTokens = tokens.subList(headerIndex, tokens.size)

            val labelToken = subTokens.filterIsInstance<Token.QuestionLabel>().firstOrNull()
            val nextHeader = subTokens.filterIsInstance<Token.QuestionHeader>().firstOrNull()

            val startPage = labelToken?.line?.pageNum ?: currentHeader.line.pageNum
            val startY = labelToken?.line?.y ?: currentHeader.line.y

            val endPage = nextHeader?.line?.pageNum ?: (startPage + 2)
            val endY = nextHeader?.line?.y

            bounds.add(
                ComprehensionBound(
                    sourceId = matchingC.sourceId,
                    startPage = startPage,
                    startY = startY,
                    endPage = endPage,
                    endY = endY
                )
            )
        }
        return bounds
    }

    private data class QuestionBound(
        val order: Int,
        val startPage: Int,
        val startY: Float,
        val endPage: Int,
        val endY: Float?,
        val optionTokens: List<Token.OptionLine>,
        /** Page of the "Options :" header, or null if the question has no options section (NAT). */
        val optionsHeaderPage: Int?,
        /** Y position of the "Options :" header on optionsHeaderPage, or null. */
        val optionsHeaderY: Float?
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

            // Find the OptionsHeader token so we can distinguish question images from option images.
            val optionsHeaderToken = qTokens.filterIsInstance<Token.OptionsHeader>().firstOrNull()

            bounds.add(
                QuestionBound(
                    order = matchingQ.order,
                    startPage = startPage,
                    startY = startY,
                    endPage = endPage,
                    endY = endY,
                    optionTokens = optionTokens,
                    optionsHeaderPage = optionsHeaderToken?.line?.pageNum,
                    optionsHeaderY = optionsHeaderToken?.line?.y
                )
            )
        }
        return bounds
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }

    /**
     * Returns true if the icon image contains at least one pixel that is
     * clearly green (R < 120, G > 120, B < 120 in 0-255 range). This
     * distinguishes a filled "correct answer" checkbox from an empty one.
     */
    private fun iconIsGreen(image: BufferedImage): Boolean {
        val rgb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return rgb.any { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            r < 120 && g > 120 && b < 120
        }
    }
}
