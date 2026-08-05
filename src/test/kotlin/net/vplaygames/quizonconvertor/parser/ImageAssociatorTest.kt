package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine
import net.vplaygames.quizonconvertor.extractor.ExtractedImage
import net.vplaygames.quizonconvertor.extractor.TextColor
import net.vplaygames.quizonconvertor.model.OptionData
import net.vplaygames.quizonconvertor.model.QuestionData
import java.awt.image.BufferedImage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImageAssociatorTest {

    /**
     * Mirrors the real PDF layout for image-heavy questions:
     *
     *   y=10   QuestionHeader
     *   y=50   [question image]          ← before "Options :" header
     *   y=100  OptionsHeader
     *   y=120  [option A image]          ← immediately before option A label
     *   y=150  OptionLine A (label)      ← gap from image ≈ image height (30f)
     *   y=250  OptionLine B (label only, no image)
     *
     * This matches the observed structure in Sem1 Maths1.pdf where option images
     * are rendered as PDF figures just above their corresponding label text lines,
     * with a gap approximately equal to the image's display height.
     */
    @Test
    fun testImageAssociationQuestionAndOption() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "quiz_img_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val lines = listOf(
                ColoredLine("Question Number : 1 Question Id : 101 Question Type : MCQ", TextColor.BLACK, 10f, 1),
                ColoredLine("Options :", TextColor.BLACK, 100f, 1),
                ColoredLine("6406533039001. Option A", TextColor.GREEN, 150f, 1),
                ColoredLine("6406533039002. Option B", TextColor.RED, 250f, 1)
            )
            val classifier = LineClassifier()
            val tokens = classifier.classifyAll(lines)

            val initialQuestions = listOf(
                QuestionData(
                    text = "Question text",
                    qType = "mcq",
                    order = 1,
                    image = null,
                    marks = 4,
                    options = listOf(
                        OptionData(1, "Option A", isCorrect = true, sourceOptionId = "6406533039001"),
                        OptionData(2, "Option B", isCorrect = false, sourceOptionId = "6406533039002")
                    ),
                    sourceQuestionId = "101",
                    sourceQuestionNumber = 1
                )
            )

            // Question image: y=50, height=30f  → before OptionsHeader (y=100) → question image
            // Option A image: y=120, height=30f → after OptionsHeader, label at y=150, gap=30 ≈ height → opt1
            // Option B: no image
            val questionImg = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
            val optAImg = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
            val iconImg = BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB)

            val images = listOf(
                // Question stem image (y=50, before OptionsHeader at y=100)
                ExtractedImage(1, 10f, 50f, 100f, 30f, questionImg, "DeviceRGB", isUiIcon = false),
                // Option A image (y=120, just before Option A label at y=150; gap=30 ≈ height=30)
                ExtractedImage(1, 10f, 120f, 100f, 30f, optAImg, "DeviceRGB", isUiIcon = false),
                // UI icon near Option A — should not affect correctness (not green pixels)
                ExtractedImage(1, 5f, 145f, 12f, 12f, iconImg, "DeviceRGB", isUiIcon = true)
            )

            val updatedQuestions = ImageAssociator.associateAndSaveImages(
                questions = initialQuestions,
                tokens = tokens,
                allImages = images,
                outputDir = tempDir,
                sectionName = "TestSection"
            )

            val q = updatedQuestions.first()

            // Question image should be assigned
            val qImg = q.image
            assertNotNull(qImg, "Question should have an image")
            assertEquals("images/TestSection/q1_img.png", qImg)

            // Option A image should be assigned (image at y=120, label at y=150, gap=30=height)
            val optA = q.options[0]
            val optAImgPath = optA.image
            assertNotNull(optAImgPath, "Option A should have an image")
            assertEquals("images/TestSection/q1_opt1.png", optAImgPath)

            // Option B has no image
            val optB = q.options[1]
            assertNull(optB.image, "Option B should have no image")

            // Verify saved files exist on disk
            assertEquals(true, File(tempDir, qImg).exists(), "Question image file should exist")
            assertEquals(true, File(tempDir, optAImgPath).exists(), "Option A image file should exist")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
