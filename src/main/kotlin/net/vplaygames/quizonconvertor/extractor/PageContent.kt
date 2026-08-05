package net.vplaygames.quizonconvertor.extractor

enum class TextColor {
    BLACK,
    GREEN,
    RED,
    UNKNOWN
}

data class ColoredChar(
    val char: String,
    val color: TextColor,
    val x: Float,
    val y: Float,
    val pageNum: Int
)

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
