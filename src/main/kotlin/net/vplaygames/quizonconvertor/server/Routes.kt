package net.vplaygames.quizonconvertor.server

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import net.vplaygames.quizonconvertor.parser.ConversionError
import net.vplaygames.quizonconvertor.parser.PdfParser
import net.vplaygames.quizonconvertor.serializer.JsonExporter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun Routing.configureRoutes() {
    get("/") {
        call.respondHtml {
            renderIndexPage()
        }
    }

    get("/api/health") {
        call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
    }

    post("/api/convert") {
        val multipart = call.receiveMultipart()

        var tempPdfFile: File? = null
        var tempOutputDir: File? = null
        var subjectCode: String? = null
        var year: Int? = null
        var term: String? = null
        var examType: String? = null
        var strict = false

        try {
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "file" || part.originalFileName?.endsWith(".pdf", ignoreCase = true) == true) {
                            val tempFile = Files.createTempFile("quizon_upload_", ".pdf").toFile()
                            part.provider().toInputStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            tempPdfFile = tempFile
                        }
                    }

                    is PartData.FormItem -> {
                        when (part.name) {
                            "subjectCode" -> subjectCode = part.value.takeIf { it.isNotBlank() }
                            "year" -> year = part.value.toIntOrNull()
                            "term" -> term = part.value.takeIf { it.isNotBlank() }
                            "examType" -> examType = part.value.takeIf { it.isNotBlank() }
                            "strict" -> strict = part.value.toBoolean()
                        }
                    }

                    else -> {}
                }
                part.release()
            }

            val pdf = tempPdfFile
            if (pdf == null || !pdf.exists() || pdf.length() == 0L) {
                call.respondText("Error: No PDF file uploaded.", status = HttpStatusCode.BadRequest)
                return@post
            }

            tempOutputDir = Files.createTempDirectory("quizon_out_").toFile()

            // Run parsing pipeline
            val parsedExports = PdfParser.parse(
                pdfFile = pdf,
                outputDir = tempOutputDir,
                strict = strict
            )

            // Apply overrides
            val updatedExports = parsedExports.map { export ->
                val updatedSub = if (subjectCode != null) export.subject.copy(code = subjectCode!!) else export.subject
                val updatedPap = export.paper.copy(
                    year = year ?: export.paper.year,
                    term = term ?: export.paper.term,
                    examType = examType ?: export.paper.examType
                )
                export.copy(subject = updatedSub, paper = updatedPap)
            }

            // Export JSON & report
            JsonExporter.export(updatedExports, tempOutputDir)

            // Zip results
            val zipBytes = zipFolder(tempOutputDir)

            val zipName = "QuizOn_Export_${System.currentTimeMillis()}.zip"
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, zipName).toString()
            )
            call.respondBytes(zipBytes, ContentType.Application.Zip)

        } catch (e: ConversionError) {
            call.respondText("Conversion Error: ${e.message}", status = HttpStatusCode.UnprocessableEntity)
        } catch (e: Exception) {
            call.respondText(
                "Server Error during conversion: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        } finally {
            tempPdfFile?.delete()
            tempOutputDir?.deleteRecursively()
        }
    }
}

private fun zipFolder(sourceFolder: File): ByteArray {
    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zos ->
        addFileToZip(sourceFolder, sourceFolder, zos)
    }
    return baos.toByteArray()
}

private fun addFileToZip(rootFolder: File, currentFile: File, zos: ZipOutputStream) {
    val files = currentFile.listFiles() ?: return
    for (file in files) {
        val relativePath = rootFolder.toPath().relativize(file.toPath()).toString().replace('\\', '/')
        if (file.isDirectory) {
            addFileToZip(rootFolder, file, zos)
        } else {
            val zipEntry = ZipEntry(relativePath)
            zos.putNextEntry(zipEntry)
            file.inputStream().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}
