package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class OptionData(
    val serial: Int,               // 1, 2, 3, 4
    val text: String,              // Option text
    val image: String? = null,     // Relative path to extracted image file
    val isCorrect: Boolean = false, // Extracted from text colour (green = true)
    val sourceOptionId: String = "" // Original platform option ID
)
