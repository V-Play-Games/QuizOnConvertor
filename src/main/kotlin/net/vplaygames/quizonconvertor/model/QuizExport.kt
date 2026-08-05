package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizExport(
    val subject: SubjectData,
    val paper: QuizPaperData,
    val questions: List<QuestionData>,
    val comprehensions: List<ComprehensionData> = emptyList(),
    val tags: List<TagData> = emptyList()
)
