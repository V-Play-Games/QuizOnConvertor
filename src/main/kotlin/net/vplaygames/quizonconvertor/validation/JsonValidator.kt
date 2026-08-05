package net.vplaygames.quizonconvertor.validation

import net.vplaygames.quizonconvertor.model.QuizExport

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

object JsonValidator {

    fun validate(export: QuizExport): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Subject validation
        if (export.subject.subject.isBlank()) {
            errors.add("Subject title is empty")
        }
        if (export.subject.code.isBlank()) {
            warnings.add("Subject code is not set")
        }

        // 2. Paper validation
        if (export.paper.title.isBlank()) {
            errors.add("Paper title is empty")
        }

        // 3. Questions validation
        if (export.questions.isEmpty()) {
            errors.add("Export contains 0 questions")
        }

        export.questions.forEachIndexed { _, question ->
            val qNum = question.order
            if (question.text.isBlank() && question.image == null) {
                errors.add("Question #$qNum has both empty text and no diagram image")
            }
            if (question.qType !in listOf("mcq", "msq", "nat")) {
                errors.add("Question #$qNum has invalid qType: '${question.qType}'")
            }

            when (question.qType) {
                "mcq", "msq" -> {
                    if (question.options.isEmpty()) {
                        errors.add("Question #$qNum (${question.qType}) has no options")
                    } else {
                        val correctCount = question.options.count { it.isCorrect }
                        if (correctCount == 0) {
                            warnings.add("Question #$qNum (${question.qType}) has no correct option identified")
                        }
                        if (question.qType == "mcq" && correctCount > 1) {
                            warnings.add("Question #$qNum (mcq) has multiple ($correctCount) correct options marked")
                        }
                    }
                }
                "nat" -> {
                    if (question.correctAnswer == null) {
                        warnings.add("Question #$qNum (nat) has no numerical answer extracted")
                    }
                }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
