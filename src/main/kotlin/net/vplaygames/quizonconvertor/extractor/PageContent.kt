package net.vplaygames.quizonconvertor.extractor

enum class TextColor {
    BLACK,
    GREEN,
    RED,
    UNKNOWN
}

data class ColoredLine(
    val text: String,
    val color: TextColor,
    val y: Float,
    val pageNum: Int
)

data class PageContent(
    val pageNumber: Int,
    val lines: List<ColoredLine>
)
