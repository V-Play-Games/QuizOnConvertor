package net.vplaygames.quizonconvertor.model

import kotlinx.serialization.Serializable

@Serializable
data class TagData(
    val tag: String  // max 20 chars
)
