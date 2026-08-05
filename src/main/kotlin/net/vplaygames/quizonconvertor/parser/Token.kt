package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine

sealed interface Token {
    val line: ColoredLine

    data class SectionHeader(override val line: ColoredLine, val sectionName: String) : Token
    data class SectionMeta(override val line: ColoredLine, val key: String, val value: String = "") : Token
    data class QuestionHeader(
        override val line: ColoredLine,
        val number: Int,
        val id: String,
        val type: String,
        val calculator: String? = null
    ) : Token
    data class ComprehensionHeader(
        override val line: ColoredLine,
        val id: String
    ) : Token
    data class ComprehensionRange(
        override val line: ColoredLine,
        val startNumber: Int,
        val endNumber: Int
    ) : Token
    data class CorrectMarks(
        override val line: ColoredLine,
        val marks: Int,
        val maxSelectableOptions: Int? = null
    ) : Token
    data class QuestionLabel(override val line: ColoredLine, val label: String) : Token
    data class SubjectTitle(override val line: ColoredLine, val title: String) : Token
    data class OptionsHeader(override val line: ColoredLine) : Token
    data class OptionLine(
        override val line: ColoredLine,
        val optionId: String,
        val optionText: String
    ) : Token
    data class PossibleAnswersHeader(override val line: ColoredLine) : Token
    data class SaMetaLine(override val line: ColoredLine, val key: String, val value: String = "") : Token
    data class SubQuestionsHeader(override val line: ColoredLine) : Token
    data class IgnoredNoise(override val line: ColoredLine) : Token
    data class FreeText(override val line: ColoredLine) : Token
}
