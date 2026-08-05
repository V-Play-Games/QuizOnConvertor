package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizPaperData(
    val title: String,             // "Sem1 Maths1"
    val year: Int,                 // CLI/GUI parameter
    val term: String,              // "jan" | "may" | "sept" — CLI/GUI parameter
    val examType: String,          // "quiz1" | "quiz2" | "endterm" — CLI/GUI parameter
    val totalDurationSeconds: Int = 0,
    val isPublished: Boolean = false
)
