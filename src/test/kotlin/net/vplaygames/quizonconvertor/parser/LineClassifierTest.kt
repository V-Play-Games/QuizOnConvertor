package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine
import net.vplaygames.quizonconvertor.extractor.TextColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineClassifierTest {

    private val classifier = LineClassifier()

    @Test
    fun testClassifyQuestionHeader() {
        val line = ColoredLine(
            text = "Question Number : 46 Question Id : 640653902325 Question Type : MSQ Calculator : Yes",
            color = TextColor.BLACK,
            y = 100f,
            pageNum = 1
        )
        val token = classifier.classify(line)
        assertTrue(token is Token.QuestionHeader)
        assertEquals(46, token.number)
        assertEquals("640653902325", token.id)
        assertEquals("MSQ", token.type)
        assertEquals("Yes", token.calculator)
    }

    @Test
    fun testClassifyComprehensionHeaderAndRange() {
        val headerLine = ColoredLine(
            text = "Question Id : 640653902328 Question Type : COMPREHENSION Sub Question Shuffling Allowed : No",
            color = TextColor.BLACK,
            y = 100f,
            pageNum = 1
        )
        val headerToken = classifier.classify(headerLine)
        assertTrue(headerToken is Token.ComprehensionHeader)
        assertEquals("640653902328", headerToken.id)

        val rangeLine = ColoredLine(
            text = "Question Numbers : (53 to 54)",
            color = TextColor.BLACK,
            y = 120f,
            pageNum = 1
        )
        val rangeToken = classifier.classify(rangeLine)
        assertTrue(rangeToken is Token.ComprehensionRange)
        assertEquals(53, rangeToken.startNumber)
        assertEquals(54, rangeToken.endNumber)
    }

    @Test
    fun testClassifyOptionLineGreenAndRed() {
        val greenLine = ColoredLine(
            text = "6406533039095. Floyd-Warshall algorithm is used for all pair shortest paths.",
            color = TextColor.GREEN,
            y = 200f,
            pageNum = 1
        )
        val greenToken = classifier.classify(greenLine)
        assertTrue(greenToken is Token.OptionLine)
        assertEquals("6406533039095", greenToken.optionId)
        assertEquals("Floyd-Warshall algorithm is used for all pair shortest paths.", greenToken.optionText)
        assertEquals(TextColor.GREEN, greenToken.line.color)

        val redLine = ColoredLine(
            text = "6406533039098. Dijkstra's algorithm is used for all pair shortest paths.",
            color = TextColor.RED,
            y = 220f,
            pageNum = 1
        )
        val redToken = classifier.classify(redLine)
        assertTrue(redToken is Token.OptionLine)
        assertEquals("6406533039098", redToken.optionId)
        assertEquals(TextColor.RED, redToken.line.color)
    }

    @Test
    fun testClassifyPossibleAnswersHeader() {
        val line = ColoredLine(
            text = "Possible Answers :",
            color = TextColor.BLACK,
            y = 300f,
            pageNum = 1
        )
        val token = classifier.classify(line)
        assertTrue(token is Token.PossibleAnswersHeader)
    }

    @Test
    fun testClassifyCorrectMarks() {
        val line = ColoredLine(
            text = "Correct Marks : 4 Max. Selectable Options : 0",
            color = TextColor.BLACK,
            y = 150f,
            pageNum = 1
        )
        val token = classifier.classify(line)
        assertTrue(token is Token.CorrectMarks)
        assertEquals(4, token.marks)
        assertEquals(0, token.maxSelectableOptions)
    }

    @Test
    fun testClassifySubjectTitle() {
        val line = ColoredLine(
            text = "THIS IS QUESTION PAPER FOR THE SUBJECT \"FOUNDATION LEVEL : SEMESTER I: MATHEMATICS FOR DATA SCIENCE I (COMPUTER BASED EXAM)\"",
            color = TextColor.BLACK,
            y = 50f,
            pageNum = 1
        )
        val token = classifier.classify(line)
        assertTrue(token is Token.SubjectTitle)
        assertEquals("FOUNDATION LEVEL : SEMESTER I: MATHEMATICS FOR DATA SCIENCE I (COMPUTER BASED EXAM)", token.title)
    }
}
