package net.vplaygames.quizonconvertor.extractor

import org.apache.pdfbox.Loader
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDPage
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File

import org.apache.pdfbox.pdmodel.graphics.image.PDImage

data class ExtractedImage(
    val pageNum: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val image: BufferedImage,
    val colorspace: String,
    val isUiIcon: Boolean
)

class ImageStreamEngine(page: PDPage, private val pageNum: Int) : PDFGraphicsStreamEngine(page) {
    val extractedImages = mutableListOf<ExtractedImage>()

    fun process() {
        processPage(page)
    }

    override fun drawImage(pdImage: PDImage) {
        val ctm = graphicsState.currentTransformationMatrix
        val image = pdImage.image
        val pageHeight = page.cropBox.height

        val x = ctm.translateX
        val pdfY = ctm.translateY
        val displayHeight = ctm.scalingFactorY
        val y = pageHeight - pdfY - displayHeight
        val displayWidth = ctm.scalingFactorX

        val colorspaceName = try {
            pdImage.colorSpace?.name ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }

        val isIcon = image.width <= 20 && image.height <= 20

        extractedImages.add(
            ExtractedImage(
                pageNum = pageNum,
                x = x,
                y = y,
                width = displayWidth,
                height = displayHeight,
                image = image,
                colorspace = colorspaceName,
                isUiIcon = isIcon
            )
        )
    }

    override fun appendRectangle(p0: Point2D?, p1: Point2D?, p2: Point2D?, p3: Point2D?) {}
    override fun clip(pathPaintingRule: Int) {}
    override fun moveTo(x: Float, y: Float) {}
    override fun lineTo(x: Float, y: Float) {}
    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {}
    override fun getCurrentPoint(): Point2D? = null
    override fun closePath() {}
    override fun endPath() {}
    override fun strokePath() {}
    override fun fillPath(windingRule: Int) {}
    override fun fillAndStrokePath(windingRule: Int) {}
    override fun shadingFill(shadingName: COSName?) {}
}

object PdfImageExtractor {
    fun extractImages(pdfFile: File): List<ExtractedImage> {
        require(pdfFile.exists()) { "PDF file does not exist: ${pdfFile.absolutePath}" }

        val allImages = mutableListOf<ExtractedImage>()
        Loader.loadPDF(pdfFile).use { document ->
            for (pageNum in 1..document.numberOfPages) {
                val page = document.getPage(pageNum - 1)
                val engine = ImageStreamEngine(page, pageNum)
                engine.process()
                allImages.addAll(engine.extractedImages)
            }
        }
        return allImages
    }
}
