package net.vplaygames.quizonconvertor.parser

import kotlinx.serialization.json.jsonPrimitive
import net.vplaygames.quizonconvertor.extractor.ColoredLine
import net.vplaygames.quizonconvertor.extractor.TextColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuestionBuilderTest {

    private val classifier = LineClassifier()
    private val builder = QuestionBuilder()

    @Test
    fun testBuildMsqQuestionWithColorAnswers() {
        val lines = listOf(
            ColoredLine("Question Number : 46 Question Id : 640653902325 Question Type : MSQ", TextColor.BLACK, 10f, 1),
            ColoredLine("Correct Marks : 4 Max. Selectable Options : 0", TextColor.BLACK, 20f, 1),
            ColoredLine("Which of the following is (are) correct?", TextColor.BLACK, 30f, 1),
            ColoredLine("Options :", TextColor.BLACK, 40f, 1),
            ColoredLine("6406533039095. Floyd-Warshall algorithm is used for all pair shortest paths.", TextColor.GREEN, 50f, 1),
            ColoredLine("6406533039096. Bellman-Ford algorithm is used for single source shortest path.", TextColor.GREEN, 60f, 1),
            ColoredLine("6406533039098. Dijkstra's algorithm is used for all pair shortest paths.", TextColor.RED, 70f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val q = questions.first()
        assertEquals("msq", q.qType)
        assertEquals(4, q.marks)
        assertEquals("640653902325", q.sourceQuestionId)
        assertEquals(46, q.sourceQuestionNumber)
        assertEquals("Which of the following is (are) correct?", q.text)
        assertEquals(3, q.options.size)

        assertTrue(q.options[0].isCorrect)
        assertEquals("Floyd-Warshall algorithm is used for all pair shortest paths.", q.options[0].text)

        assertTrue(q.options[1].isCorrect)
        assertEquals("Bellman-Ford algorithm is used for single source shortest path.", q.options[1].text)

        assertEquals(false, q.options[2].isCorrect)
        assertEquals("Dijkstra's algorithm is used for all pair shortest paths.", q.options[2].text)
    }

    @Test
    fun testBuildSaQuestionWithAnswerValue() {
        val lines = listOf(
            ColoredLine("Question Number : 51 Question Id : 640653902327 Question Type : SA", TextColor.BLACK, 10f, 1),
            ColoredLine("Correct Marks : 4", TextColor.BLACK, 20f, 1),
            ColoredLine("Response Type : Numeric", TextColor.BLACK, 30f, 1),
            ColoredLine("Possible Answers :", TextColor.BLACK, 40f, 1),
            ColoredLine("35", TextColor.GREEN, 50f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val q = questions.first()
        assertEquals("nat", q.qType)
        assertEquals(4, q.marks)
        val ans = q.correctAnswer
        assertNotNull(ans)
        assertEquals("35", ans.jsonPrimitive.content)
    }

    @Test
    fun testBuildSaQuestionWithRangeAnswer() {
        val lines = listOf(
            ColoredLine("Question Number : 52 Question Id : 640653902328 Question Type : SA", TextColor.BLACK, 10f, 1),
            ColoredLine("Possible Answers :", TextColor.BLACK, 20f, 1),
            ColoredLine("10 to 10", TextColor.BLACK, 30f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val q = questions.first()
        assertEquals("nat", q.qType)
        val ans = q.correctAnswer
        assertNotNull(ans)
        assertEquals("10", ans.jsonPrimitive.content)
        assertEquals(null, q.natTolerance)
    }

    @Test
    fun testBuildSaQuestionWithInlineRangeAnswer() {
        val lines = listOf(
            ColoredLine("Question Number : 53 Question Id : 640653902329 Question Type : SA", TextColor.BLACK, 10f, 1),
            ColoredLine("Possible Answers : 10.5 to 10.5", TextColor.BLACK, 20f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val q = questions.first()
        assertEquals("nat", q.qType)
        val ans = q.correctAnswer
        assertNotNull(ans)
        assertEquals("10.5", ans.jsonPrimitive.content)
        assertEquals(null, q.natTolerance)
    }

    @Test
    fun testBuildSaQuestionWithRangeAndTolerance() {
        val lines = listOf(
            ColoredLine("Question Number : 54 Question Id : 640653902330 Question Type : SA", TextColor.BLACK, 10f, 1),
            ColoredLine("Possible Answers : 10 to 12", TextColor.BLACK, 20f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val q = questions.first()
        assertEquals("nat", q.qType)
        val ans = q.correctAnswer
        assertNotNull(ans)
        assertEquals("10", ans.jsonPrimitive.content)
        assertEquals(2.0, q.natTolerance)
    }

    @Test
    fun testBuildComprehensionSubQuestions() {
        val lines = listOf(
            ColoredLine("Question Id : 640653902328 Question Type : COMPREHENSION", TextColor.BLACK, 10f, 1),
            ColoredLine("Question Numbers : (53 to 54)", TextColor.BLACK, 20f, 1),
            ColoredLine("Text above question label to be ignored", TextColor.BLACK, 21f, 1),
            ColoredLine("Question Label : Comprehension", TextColor.BLACK, 22f, 1),
            ColoredLine("Read the following passage carefully:", TextColor.BLACK, 23f, 1),
            ColoredLine("This is paragraph 1 of the comprehension passage.", TextColor.BLACK, 24f, 1),
            ColoredLine("Sub questions", TextColor.BLACK, 26f, 1),
            ColoredLine("Question Number : 53 Question Id : 640653902329 Question Type : MCQ", TextColor.BLACK, 30f, 1),
            ColoredLine("Correct Marks : 4", TextColor.BLACK, 40f, 1),
            ColoredLine("Options :", TextColor.BLACK, 50f, 1),
            ColoredLine("6406533039105. Option A text", TextColor.GREEN, 60f, 1),
            ColoredLine("Question Number : 54 Question Id : 640653902330 Question Type : SA", TextColor.BLACK, 70f, 1),
            ColoredLine("Correct Marks : 2", TextColor.BLACK, 80f, 1),
            ColoredLine("Possible Answers :", TextColor.BLACK, 90f, 1),
            ColoredLine("108", TextColor.GREEN, 100f, 1)
        )

        val tokens = classifier.classifyAll(lines)
        val buildResult = builder.buildAll(tokens)

        assertEquals(2, buildResult.questions.size)
        assertEquals("640653902328", buildResult.questions[0].comprehensionParentId)
        assertEquals("640653902328", buildResult.questions[1].comprehensionParentId)
        assertEquals("mcq", buildResult.questions[0].qType)
        assertEquals("nat", buildResult.questions[1].qType)

        assertEquals(1, buildResult.comprehensions.size)
        val comp = buildResult.comprehensions.first()
        assertEquals("640653902328", comp.sourceId)
        assertEquals("Read the following passage carefully:\nThis is paragraph 1 of the comprehension passage.", comp.text)
        assertEquals(listOf(53, 54), comp.questionNumbers)
    }

    @Test
    fun testMultiLineOptionContinuation() {
        val lines = listOf(
            ColoredLine("Question Number : 46 Question Id : 640653902325 Question Type : MSQ", TextColor.BLACK, 10f, 1),
            ColoredLine("Options :", TextColor.BLACK, 20f, 1),
            ColoredLine("6406533039096. The Shortest path problem is not applicable to a graph with a negative weight", TextColor.GREEN, 30f, 1),
            ColoredLine("cycle.", TextColor.GREEN, 40f, 2)
        )

        val tokens = classifier.classifyAll(lines)
        val questions = builder.buildQuestions(tokens)

        assertEquals(1, questions.size)
        val opt = questions.first().options.first()
        assertEquals("The Shortest path problem is not applicable to a graph with a negative weight cycle.", opt.text)
        assertTrue(opt.isCorrect)
    }
}
