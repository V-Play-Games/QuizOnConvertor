package net.vplaygames.quizonconvertor.parser

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.vplaygames.quizonconvertor.extractor.TextColor
import net.vplaygames.quizonconvertor.model.ComprehensionData
import net.vplaygames.quizonconvertor.model.OptionData
import net.vplaygames.quizonconvertor.model.QuestionData

data class BuildResult(
    val questions: List<QuestionData>,
    val comprehensions: List<ComprehensionData>
)

class QuestionBuilder {

    private enum class State {
        IDLE,
        READING_QUESTION_TEXT,
        READING_OPTIONS,
        READING_SA_ANSWER,
        READING_COMPREHENSION_TEXT
    }

    private data class MutableOption(
        val serial: Int,
        var text: String,
        var isCorrect: Boolean,
        val sourceOptionId: String
    )

    private data class MutableComprehension(
        var sourceId: String = "",
        val textLines: MutableList<String> = mutableListOf(),
        var startNumber: Int = 0,
        var endNumber: Int = 0
    ) {
        fun build(): ComprehensionData {
            val qNums = if (startNumber > 0 && endNumber >= startNumber) (startNumber..endNumber).toList() else emptyList()
            return ComprehensionData(
                sourceId = sourceId,
                text = textLines.joinToString("\n").trim(),
                image = null,
                questionNumbers = qNums
            )
        }
    }

    private data class MutableQuestion(
        var sourceQuestionNumber: Int = 0,
        var sourceQuestionId: String = "",
        var qType: String = "mcq",
        var marks: Int = 0,
        var negativeMarks: Int = 0,
        var comprehensionParentId: String? = null,
        val textLines: MutableList<String> = mutableListOf(),
        val options: MutableList<MutableOption> = mutableListOf(),
        var correctAnswer: JsonElement? = null,
        var natTolerance: Double? = null
    ) {
        fun build(order: Int): QuestionData {
            val cleanText = textLines.joinToString("\n").trim()
            return QuestionData(
                text = cleanText,
                qType = qType,
                order = order,
                image = null,
                correctAnswer = correctAnswer,
                explanation = "",
                marks = marks,
                negativeMarks = negativeMarks,
                natTolerance = natTolerance,
                options = options.map {
                    OptionData(
                        serial = it.serial,
                        text = it.text.trim(),
                        image = null,
                        isCorrect = it.isCorrect,
                        sourceOptionId = it.sourceOptionId
                    )
                },
                sourceQuestionId = sourceQuestionId,
                sourceQuestionNumber = sourceQuestionNumber,
                comprehensionParentId = comprehensionParentId
            )
        }
    }

    private fun parseAndSetNatAnswer(q: MutableQuestion, rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        val primaryText = trimmed.split(",").first().trim()

        val singleNum = primaryText.toDoubleOrNull()
        if (singleNum != null) {
            q.correctAnswer = if (singleNum % 1.0 == 0.0) {
                JsonPrimitive(singleNum.toLong())
            } else {
                JsonPrimitive(singleNum)
            }
            return
        }

        val rangeRegex = Regex("""^([+-]?\d+(?:\.\d+)?)\s*(?:to|-|:)\s*([+-]?\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE)
        val match = rangeRegex.find(primaryText)
        if (match != null) {
            val minVal = match.groupValues[1].toDoubleOrNull()
            val maxVal = match.groupValues[2].toDoubleOrNull()
            if (minVal != null && maxVal != null) {
                val min = minOf(minVal, maxVal)
                val max = maxOf(minVal, maxVal)

                q.correctAnswer = if (min % 1.0 == 0.0) {
                    JsonPrimitive(min.toLong())
                } else {
                    JsonPrimitive(min)
                }

                if (min != max) {
                    val tol = max - min
                    q.natTolerance = if (tol % 1.0 == 0.0) tol else (Math.round(tol * 10000.0) / 10000.0)
                }
                return
            }
        }

        q.correctAnswer = JsonPrimitive(primaryText)
    }

    fun buildQuestions(tokens: List<Token>): List<QuestionData> {
        return buildAll(tokens).questions
    }

    fun buildAll(tokens: List<Token>): BuildResult {
        val questions = mutableListOf<QuestionData>()
        val comprehensions = mutableListOf<ComprehensionData>()

        var currentQuestion: MutableQuestion? = null
        var currentComprehension: MutableComprehension? = null
        var currentState = State.IDLE

        var activeComprehensionId: String? = null
        var activeComprehensionRange: IntRange? = null

        fun flushCurrentQuestion() {
            currentQuestion?.let { q ->
                if (q.sourceQuestionNumber > 0 && q.sourceQuestionId.isNotEmpty()) {
                    questions.add(q.build(order = questions.size + 1))
                }
            }
            currentQuestion = null
        }

        fun flushCurrentComprehension() {
            currentComprehension?.let { c ->
                if (c.sourceId.isNotEmpty()) {
                    comprehensions.add(c.build())
                }
            }
            currentComprehension = null
        }

        for (token in tokens) {
            when (token) {
                is Token.QuestionHeader -> {
                    flushCurrentQuestion()
                    flushCurrentComprehension()

                    // Clear comprehension if current question number is beyond range
                    val range = activeComprehensionRange
                    if (range != null && token.number !in range) {
                        activeComprehensionId = null
                        activeComprehensionRange = null
                    }

                    val qTypeMapped = when (token.type) {
                        "MCQ" -> "mcq"
                        "MSQ" -> "msq"
                        "SA" -> "nat"
                        else -> token.type.lowercase()
                    }

                    val parentId = if (range != null && token.number in range) {
                        activeComprehensionId
                    } else null

                    currentQuestion = MutableQuestion(
                        sourceQuestionNumber = token.number,
                        sourceQuestionId = token.id,
                        qType = qTypeMapped,
                        comprehensionParentId = parentId
                    )
                    currentState = State.READING_QUESTION_TEXT
                }

                is Token.ComprehensionHeader -> {
                    flushCurrentQuestion()
                    flushCurrentComprehension()
                    activeComprehensionId = token.id
                    currentComprehension = MutableComprehension(sourceId = token.id)
                    currentState = State.READING_COMPREHENSION_TEXT
                }

                is Token.ComprehensionRange -> {
                    activeComprehensionRange = token.startNumber..token.endNumber
                    currentComprehension?.let { c ->
                        c.startNumber = token.startNumber
                        c.endNumber = token.endNumber
                    }
                }

                is Token.SubQuestionsHeader -> {
                    if (currentState == State.READING_COMPREHENSION_TEXT) {
                        currentState = State.IDLE
                    }
                }

                is Token.CorrectMarks -> {
                    currentQuestion?.marks = token.marks
                }

                is Token.OptionsHeader -> {
                    currentState = State.READING_OPTIONS
                }

                is Token.OptionLine -> {
                    currentState = State.READING_OPTIONS
                    currentQuestion?.let { q ->
                        val isGreen = (token.line.color == TextColor.GREEN)
                        q.options.add(
                            MutableOption(
                                serial = q.options.size + 1,
                                text = token.optionText,
                                isCorrect = isGreen,
                                sourceOptionId = token.optionId
                            )
                        )
                    }
                }

                is Token.PossibleAnswersHeader -> {
                    currentState = State.READING_SA_ANSWER
                    token.inlineAnswer?.let { ansText ->
                        currentQuestion?.let { q ->
                            parseAndSetNatAnswer(q, ansText)
                        }
                        currentState = State.IDLE
                    }
                }

                is Token.FreeText -> {
                    when (currentState) {
                        State.READING_COMPREHENSION_TEXT -> {
                            currentComprehension?.textLines?.add(token.line.text)
                        }

                        State.READING_QUESTION_TEXT -> {
                            currentQuestion?.textLines?.add(token.line.text)
                        }

                        State.READING_OPTIONS -> {
                            currentQuestion?.options?.lastOrNull()?.let { opt ->
                                opt.text = if (opt.text.isEmpty()) token.line.text else "${opt.text} ${token.line.text}"
                                if (token.line.color == TextColor.GREEN) {
                                    opt.isCorrect = true
                                }
                            }
                        }

                        State.READING_SA_ANSWER -> {
                            currentQuestion?.let { q ->
                                parseAndSetNatAnswer(q, token.line.text)
                            }
                            currentState = State.IDLE
                        }

                        State.IDLE -> {
                            // Ignored or preamble
                        }
                    }
                }

                else -> {
                    // Ignore metadata & noise tokens in question building
                }
            }
        }

        flushCurrentQuestion()
        flushCurrentComprehension()
        return BuildResult(questions = questions, comprehensions = comprehensions)
    }
}
