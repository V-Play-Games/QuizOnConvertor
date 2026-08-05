package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class ComprehensionData(
    val sourceId: String,
    val text: String,
    val image: String? = null,
    val questionNumbers: List<Int> = emptyList()
)
