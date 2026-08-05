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

            val dummyImg = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
            val iconImg = BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB)

            val images = listOf(
                ExtractedImage(1, 10f, 50f, 100f, 100f, dummyImg, "DeviceRGB", isUiIcon = false), // Question img (Y=50)
                ExtractedImage(1, 10f, 180f, 100f, 100f, dummyImg, "DeviceRGB", isUiIcon = false), // Option A img (Y=180)
                ExtractedImage(1, 5f, 150f, 12f, 12f, iconImg, "DeviceRGB", isUiIcon = true) // UI Icon (should be ignored)
            )

            val updatedQuestions = ImageAssociator.associateAndSaveImages(
                questions = initialQuestions,
                tokens = tokens,
                allImages = images,
                outputDir = tempDir,
                sectionName = "TestSection"
            )

            val q = updatedQuestions.first()
            val qImg = q.image
            assertNotNull(qImg)
            assertEquals("images/TestSection/q1_img.png", qImg)

            val optA = q.options[0]
            val optAImg = optA.image
            assertNotNull(optAImg)
            assertEquals("images/TestSection/q1_opt1.png", optAImg)

            val optB = q.options[1]
            assertNull(optB.image)

            // Verify saved files exist
            val qImgFile = File(tempDir, qImg)
            val optAImgFile = File(tempDir, optAImg)
            assertEquals(true, qImgFile.exists())
            assertEquals(true, optAImgFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
