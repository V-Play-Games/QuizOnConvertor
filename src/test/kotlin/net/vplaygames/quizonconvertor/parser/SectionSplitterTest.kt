package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine
import net.vplaygames.quizonconvertor.extractor.PageContent
import net.vplaygames.quizonconvertor.extractor.TextColor
import kotlin.test.Test
import kotlin.test.assertEquals

class SectionSplitterTest {

    @Test
    fun testSplitSections() {
        val lines = listOf(
            ColoredLine("Sem1 Maths1", TextColor.BLACK, 10f, 1),
            ColoredLine("Section Id :", TextColor.BLACK, 20f, 1),
            ColoredLine("64065364071", TextColor.BLACK, 30f, 1),
            ColoredLine("Question Number : 1 Question Id : 101 Question Type : MCQ", TextColor.BLACK, 40f, 1),
            ColoredLine("Sem1 Statistics1", TextColor.BLACK, 50f, 2),
            ColoredLine("Section Id :", TextColor.BLACK, 60f, 2),
            ColoredLine("64065364072", TextColor.BLACK, 70f, 2)
        )
        val pages = listOf(
            PageContent(1, lines.subList(0, 4)),
            PageContent(2, lines.subList(4, 7))
        )

        val sections = SectionSplitter.splitSections(pages)

        assertEquals(2, sections.size)
        assertEquals("Sem1 Maths1", sections[0].sectionName)
        assertEquals(4, sections[0].lines.size)

        assertEquals("Sem1 Statistics1", sections[1].sectionName)
        assertEquals(3, sections[1].lines.size)
    }
}
