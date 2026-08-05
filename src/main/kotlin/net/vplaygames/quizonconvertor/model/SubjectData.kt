package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class SubjectData(
    val subject: String,           // "Mathematics for Data Science I"
    val code: String,              // "MAT101" — CLI/GUI parameter
    val level: String,             // "Foundation" | "Diploma" | "Degree"
    val description: String = "",
    val icon: String = ""
)
