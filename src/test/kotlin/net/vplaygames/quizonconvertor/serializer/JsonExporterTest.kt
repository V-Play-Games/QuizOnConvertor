package net.vplaygames.quizonconvertor.serializer

import kotlinx.serialization.json.Json
import net.vplaygames.quizonconvertor.model.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonExporterTest {

    @Test
    fun testExportJsonAndReport() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "json_export_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val export = QuizExport(
                subject = SubjectData(
                    subject = "Mathematics for Data Science I",
                    code = "MAT101",
                    level = "Foundation"
                ),
                paper = QuizPaperData(
                    title = "Sem1 Maths1",
                    year = 2025,
                    term = "may",
                    examType = "endterm"
                ),
                questions = listOf(
                    QuestionData(
                        text = "Which statement is true?",
                        qType = "mcq",
                        order = 1,
                        marks = 4,
                        options = listOf(
                            OptionData(1, "Option A", isCorrect = true, sourceOptionId = "6406533039001"),
                            OptionData(2, "Option B", isCorrect = false, sourceOptionId = "6406533039002")
                        ),
                        sourceQuestionId = "101",
                        sourceQuestionNumber = 1
                    )
                )
            )

            val generatedFiles = JsonExporter.export(
                exports = listOf(export),
                outputDir = tempDir,
                prettyPrint = true,
                warnings = listOf("Sample diagnostic warning")
            )

            assertEquals(2, generatedFiles.size)

            val jsonFile = File(tempDir, "Sem1_Maths1.json")
            val reportFile = File(tempDir, "conversion_report.txt")

            assertTrue(jsonFile.exists())
            assertTrue(reportFile.exists())

            // Validate deserialization of produced JSON
            val jsonContent = jsonFile.readText()
            val deserialized = Json.decodeFromString(QuizExport.serializer(), jsonContent)

            assertEquals("Mathematics for Data Science I", deserialized.subject.subject)
            assertEquals("MAT101", deserialized.subject.code)
            assertEquals("Sem1 Maths1", deserialized.paper.title)
            assertEquals(1, deserialized.questions.size)
            assertEquals(true, deserialized.questions.first().options.first().isCorrect)

            // Validate report content
            val reportContent = reportFile.readText()
            assertTrue(reportContent.contains("QuizOnConvertor Conversion Report"))
            assertTrue(reportContent.contains("Sem1 Maths1"))
            assertTrue(reportContent.contains("Sample diagnostic warning"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
