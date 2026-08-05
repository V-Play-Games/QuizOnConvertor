package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine
import net.vplaygames.quizonconvertor.extractor.PageContent

data class SectionContent(
    val sectionName: String,
    val lines: List<ColoredLine>
)

object SectionSplitter {

    fun splitSections(pages: List<PageContent>): List<SectionContent> {
        val allLines = pages.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()

        val sections = mutableListOf<SectionContent>()

        var currentSectionName: String? = null
        val currentLines = mutableListOf<ColoredLine>()

        fun isSectionHeader(index: Int): Boolean {
            val lineText = allLines[index].text.trim()
            if (Patterns.SECTION_NAME.matches(lineText)) return true
            if (index + 1 < allLines.size) {
                val nextText = allLines[index + 1].text.trim()
                if (nextText.startsWith("Section Id", ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        for (i in allLines.indices) {
            if (isSectionHeader(i)) {
                if (currentLines.isNotEmpty() && currentSectionName != null) {
                    sections.add(SectionContent(currentSectionName, currentLines.toList()))
                    currentLines.clear()
                }
                currentSectionName = allLines[i].text.trim()
                currentLines.add(allLines[i])
            } else {
                if (currentSectionName == null) {
                    currentSectionName = allLines[i].text.trim()
                }
                currentLines.add(allLines[i])
            }
        }

        if (currentLines.isNotEmpty() && currentSectionName != null) {
            sections.add(SectionContent(currentSectionName, currentLines.toList()))
        }

        return sections
    }
}
