package net.vplaygames.quizonconvertor.parser

import net.vplaygames.quizonconvertor.extractor.ColoredLine

object Patterns {
    val QUESTION_HEADER = Regex(
        """Question Number\s*:\s*(\d+)\s+Question Id\s*:\s*(\d+)\s+Question Type\s*:\s*(\w+)(?:\s+Calculator\s*:\s*(\w+))?""",
        RegexOption.IGNORE_CASE
    )
    val COMPREHENSION_HEADER = Regex(
        """Question Id\s*:\s*(\d+)\s+Question Type\s*:\s*COMPREHENSION""",
        RegexOption.IGNORE_CASE
    )
    val COMPREHENSION_RANGE = Regex(
        """Question Numbers\s*:\s*\(\s*(\d+)\s+to\s+(\d+)\s*\)""",
        RegexOption.IGNORE_CASE
    )
    val CORRECT_MARKS = Regex(
        """Correct Marks\s*:\s*(\d+)(?:\s+Max\.\s*Selectable\s*Options\s*:\s*(\d+))?""",
        RegexOption.IGNORE_CASE
    )
    val QUESTION_LABEL = Regex(
        """Question Label\s*:\s*(.+)""",
        RegexOption.IGNORE_CASE
    )
    val OPTIONS_HEADER = Regex(
        """^Options\s*:?$""",
        RegexOption.IGNORE_CASE
    )
    val OPTION_LINE = Regex(
        """^(\d{10,})\.\s*(.*)$"""
    )
    val POSSIBLE_ANSWERS = Regex(
        """^Possible Answers\s*:?$""",
        RegexOption.IGNORE_CASE
    )
    val SUBJECT_TITLE = Regex(
        """SUBJECT\s+"([^"]+)"""",
        RegexOption.IGNORE_CASE
    )
    val SECTION_NAME = Regex(
        """^(Sem\d+\s+.*)$""",
        RegexOption.IGNORE_CASE
    )
    val SECTION_META = Regex(
        """^(Section Id|Section Number|Section type|Mandatory or Optional|Number of Questions|Number of Questions to be attempted|Section Marks|Display Number Panel|Section Negative Marks|Group All Questions|Enable Mark as Answered.*|Clear Response|Maximum Instruction Time|Sub-Section Number|Sub-Section Id|Question Shuffling Allowed)\s*:?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    val SA_META = Regex(
        """^(Response Type|Evaluation Required For SA|Show Word Count|Answers Type|Text Areas)\s*:?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    val SUB_QUESTIONS = Regex(
        """^Sub questions$""",
        RegexOption.IGNORE_CASE
    )
}

class LineClassifier {
    fun classify(line: ColoredLine, prevToken: Token? = null): Token {
        val trimmed = line.text.trim()
        if (trimmed.isEmpty()) {
            return Token.IgnoredNoise(line)
        }

        // 1. Question Header
        Patterns.QUESTION_HEADER.find(trimmed)?.let { match ->
            val (num, id, type) = match.destructured
            val calc = match.groupValues.getOrNull(4)
            return Token.QuestionHeader(
                line = line,
                number = num.toInt(),
                id = id,
                type = type.uppercase(),
                calculator = calc
            )
        }

        // 2. Comprehension Header
        Patterns.COMPREHENSION_HEADER.find(trimmed)?.let { match ->
            val id = match.groupValues[1]
            return Token.ComprehensionHeader(line = line, id = id)
        }

        // 3. Comprehension Range
        Patterns.COMPREHENSION_RANGE.find(trimmed)?.let { match ->
            val (start, end) = match.destructured
            return Token.ComprehensionRange(
                line = line,
                startNumber = start.toInt(),
                endNumber = end.toInt()
            )
        }

        // 4. Correct Marks
        Patterns.CORRECT_MARKS.find(trimmed)?.let { match ->
            val marks = match.groupValues[1].toInt()
            val maxSel = match.groupValues.getOrNull(2)?.toIntOrNull()
            return Token.CorrectMarks(line = line, marks = marks, maxSelectableOptions = maxSel)
        }

        // 5. Question Label
        Patterns.QUESTION_LABEL.find(trimmed)?.let { match ->
            return Token.QuestionLabel(line = line, label = match.groupValues[1].trim())
        }

        // 6. Options Header
        if (Patterns.OPTIONS_HEADER.matches(trimmed)) {
            return Token.OptionsHeader(line)
        }

        // 7. Option Line
        Patterns.OPTION_LINE.find(trimmed)?.let { match ->
            val (optId, optText) = match.destructured
            return Token.OptionLine(
                line = line,
                optionId = optId,
                optionText = optText.trim()
            )
        }

        // 8. Possible Answers Header
        if (Patterns.POSSIBLE_ANSWERS.matches(trimmed)) {
            return Token.PossibleAnswersHeader(line)
        }

        // 9. Subject Title
        Patterns.SUBJECT_TITLE.find(trimmed)?.let { match ->
            return Token.SubjectTitle(line = line, title = match.groupValues[1].trim())
        }
        if (trimmed.contains("FOR THE SUBJECT \"", ignoreCase = true)) {
            val titleStart = trimmed.substringAfter("FOR THE SUBJECT \"", "").replace("\"", "").trim()
            return Token.SubjectTitle(line = line, title = titleStart)
        }

        // 10. Sub questions
        if (Patterns.SUB_QUESTIONS.matches(trimmed)) {
            return Token.SubQuestionsHeader(line)
        }

        // 11. SA Meta
        Patterns.SA_META.find(trimmed)?.let { match ->
            return Token.SaMetaLine(line = line, key = match.groupValues[1], value = match.groupValues[2].trim())
        }

        // 12. Section Meta
        Patterns.SECTION_META.find(trimmed)?.let { match ->
            return Token.SectionMeta(line = line, key = match.groupValues[1], value = match.groupValues[2].trim())
        }

        // If previous token was a key-only Meta token, this line might be its standalone value
        if (prevToken is Token.SectionMeta && prevToken.value.isEmpty()) {
            return Token.IgnoredNoise(line)
        }
        if (prevToken is Token.SaMetaLine && prevToken.value.isEmpty()) {
            return Token.IgnoredNoise(line)
        }

        // Default: FreeText
        return Token.FreeText(line)
    }

    fun classifyAll(lines: List<ColoredLine>): List<Token> {
        val tokens = mutableListOf<Token>()
        var prevToken: Token? = null
        for (line in lines) {
            val token = classify(line, prevToken)
            tokens.add(token)
            prevToken = token
        }
        return tokens
    }
}
