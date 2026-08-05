package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuestionData(
    val text: String,              // Question text (may be empty if image-only)
    val qType: String,             // "nat" | "mcq" | "msq"
    val order: Int,                // Sequential within paper
    val image: String? = null,     // Relative path to extracted image file
    val correctAnswer: JsonElement? = null,  // For NAT: {"value": 3}, MCQ/MSQ: derived from options
    val explanation: String = "",
    val marks: Int,
    val negativeMarks: Int = 0,
    val codeSnippet: String? = null,
    val natTolerance: Double? = null,
    val referenceTags: List<String> = emptyList(),
    val options: List<OptionData> = emptyList(),
    val sourceQuestionId: String = "",    // Original platform question ID for traceability
    val sourceQuestionNumber: Int = 0,    // Original numbering in PDF
    val comprehensionParentId: String? = null  // Links sub-questions to parent
)
