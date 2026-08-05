package net.vplaygames.quizonconvertor.extractor

import org.apache.pdfbox.contentstream.operator.color.*
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.util.IdentityHashMap

class ColorTextStripper : PDFTextStripper() {

    private val positionColorMap = IdentityHashMap<TextPosition, TextColor>()
    private val currentLines = mutableListOf<ColoredLine>()
    val pagesContent = mutableListOf<PageContent>()

    init {
        sortByPosition = true

        // Register color operators so graphicsState tracks color changes during stream parsing
        addOperator(SetNonStrokingColor(this))
        addOperator(SetNonStrokingColorSpace(this))
        addOperator(SetNonStrokingDeviceRGBColor(this))
        addOperator(SetNonStrokingDeviceGrayColor(this))
        addOperator(SetNonStrokingDeviceCMYKColor(this))
        addOperator(SetStrokingColor(this))
        addOperator(SetStrokingColorSpace(this))
        addOperator(SetStrokingDeviceRGBColor(this))
        addOperator(SetStrokingDeviceGrayColor(this))
        addOperator(SetStrokingDeviceCMYKColor(this))
        addOperator(SetNonStrokingColorN(this))
        addOperator(SetStrokingColorN(this))
    }

    override fun processTextPosition(text: TextPosition) {
        val gs = graphicsState
        val textColor = classifyColor(gs.nonStrokingColor)
        positionColorMap[text] = textColor
        super.processTextPosition(text)
    }

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        if (text.trim().isNotEmpty() && textPositions.isNotEmpty()) {
            val lineY = textPositions.first().yDirAdj
            val lineColor = determineLineColor(textPositions)
            currentLines.add(
                ColoredLine(
                    text = text,
                    color = lineColor,
                    y = lineY,
                    pageNum = currentPageNo
                )
            )
        }
        super.writeString(text, textPositions)
    }

    override fun startPage(page: PDPage) {
        currentLines.clear()
        super.startPage(page)
    }

    override fun endPage(page: PDPage) {
        super.endPage(page)
        pagesContent.add(PageContent(currentPageNo, currentLines.toList()))
    }

    private fun determineLineColor(positions: List<TextPosition>): TextColor {
        var greenCount = 0
        var redCount = 0
        var blackCount = 0

        for (pos in positions) {
            when (positionColorMap[pos]) {
                TextColor.GREEN -> greenCount++
                TextColor.RED -> redCount++
                TextColor.BLACK -> blackCount++
                else -> {}
            }
        }

        return when {
            greenCount > 0 && greenCount >= redCount -> TextColor.GREEN
            redCount > 0 -> TextColor.RED
            else -> TextColor.BLACK
        }
    }

    private fun classifyColor(pdColor: PDColor?): TextColor {
        if (pdColor == null) return TextColor.UNKNOWN
        return try {
            val colorSpace = pdColor.colorSpace ?: return TextColor.UNKNOWN
            val components = pdColor.components ?: return TextColor.UNKNOWN
            val rgb = colorSpace.toRGB(components) ?: return TextColor.UNKNOWN
            if (rgb.size < 3) return TextColor.UNKNOWN

            val r = rgb[0]
            val g = rgb[1]
            val b = rgb[2]

            when {
                // Green: (0.0, ~0.502, 0.0) in RGB
                r < 0.2f && g in 0.35f..0.75f && b < 0.2f -> TextColor.GREEN
                // Bright Green:
                r < 0.2f && g > 0.75f && b < 0.2f -> TextColor.GREEN
                // Red: (1.0, 0.0, 0.0) in RGB
                r > 0.8f && g < 0.2f && b < 0.2f -> TextColor.RED
                // Black: (0.0, 0.0, 0.0) in RGB or Grayscale 0
                r < 0.2f && g < 0.2f && b < 0.2f -> TextColor.BLACK
                else -> TextColor.UNKNOWN
            }
        } catch (_: Exception) {
            TextColor.UNKNOWN
        }
    }
}
