package net.vplaygames.quizonconvertor.validation

import net.vplaygames.quizonconvertor.model.*
import net.vplaygames.quizonconvertor.parser.PdfParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {

    @Test
    fun testJsonValidatorValidExport() {
        val validExport = QuizExport(
            subject = SubjectData("Mathematics", "MAT101", "Foundation"),
            paper = QuizPaperData("Sem1 Maths1", 2025, "may", "endterm"),
            questions = listOf(
                QuestionData(
                    text = "What is 2+2?",
                    qType = "mcq",
                    order = 1,
                    marks = 4,
                    options = listOf(
                        OptionData(1, "4", isCorrect = true),
                        OptionData(2, "5", isCorrect = false)
                    )
                )
            )
        )

        val result = JsonValidator.validate(validExport)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testJsonValidatorInvalidExport() {
        val invalidExport = QuizExport(
            subject = SubjectData("", "", "Foundation"),
            paper = QuizPaperData("", 2025, "may", "endterm"),
            questions = listOf(
                QuestionData(
                    text = "",
                    qType = "invalid_type",
                    order = 1,
                    marks = 0,
                    options = emptyList()
                )
            )
        )

        val result = JsonValidator.validate(invalidExport)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Subject title is empty") })
        assertTrue(result.errors.any { it.contains("invalid qType") })
    }

    @Test
    fun testSpecimenAnswerKeyAccuracyAndValidation() {
        val pdfFile = File("Sem1 Maths1.pdf")
        if (!pdfFile.exists()) return

        val tempOut = File(System.getProperty("java.io.tmpdir"), "val_specimen_${System.currentTimeMillis()}")
        tempOut.mkdirs()

        try {
            val exports = PdfParser.parse(pdfFile, tempOut)
            assertEquals(1, exports.size)

            val export = exports.first()
            val valResult = JsonValidator.validate(export)

            assertTrue(valResult.isValid, "Export should be valid, errors: ${valResult.errors}")
            assertEquals(18, export.questions.size)

            val mcqCount = export.questions.count { it.qType == "mcq" }
            val msqCount = export.questions.count { it.qType == "msq" }
            val natCount = export.questions.count { it.qType == "nat" }

            assertEquals(6, mcqCount)
            assertEquals(6, msqCount)
            assertEquals(6, natCount)

            // 19 is the correct count from the PDF answer key.
            // The previous value (32) was inflated by the buggy CalRGB-colorspace fallback
            // which marked blank-text options as correct whenever any CalRGB icon appeared
            // nearby — which was true for both correct AND incorrect options in this PDF.
            // The pixel-based green-detection fix produces the accurate figure.
            val totalCorrectOpts = export.questions.flatMap { it.options }.count { it.isCorrect }
            assertEquals(19, totalCorrectOpts)

            val natAnswers = export.questions.count { it.correctAnswer != null }
            assertEquals(6, natAnswers)

        } finally {
            tempOut.deleteRecursively()
        }
    }
}
