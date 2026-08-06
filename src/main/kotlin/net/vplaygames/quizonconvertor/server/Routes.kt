package net.vplaygames.quizonconvertor.server

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
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
        var subjectCode: String? = null
        var year: Int? = null
        var term: String? = null
        var examType: String? = null
        var strict = false

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

        val job = JobRegistry.create()

        val sCode = subjectCode
        val yr = year
        val tm = term
        val eType = examType
        val st = strict

        CoroutineScope(Dispatchers.IO).launch {
            var tempOutputDir: File? = null
            try {
                job.status = JobStatus.RUNNING
                job.emit("Uploading PDF", "Processing uploaded file...", 2)

                tempOutputDir = Files.createTempDirectory("quizon_out_").toFile()

                val parsedExports = PdfParser.parse(
                    pdfFile = pdf,
                    outputDir = tempOutputDir,
                    strict = st,
                    progressCallback = { step, detail, percent ->
                        job.emit(step, detail, percent)
                    }
                )

                job.emit("Formatting output", "Applying metadata and creating JSON models...", 92)
                val updatedExports = parsedExports.map { export ->
                    val updatedSub = if (sCode != null) export.subject.copy(code = sCode) else export.subject
                    val updatedPap = export.paper.copy(
                        year = yr ?: export.paper.year,
                        term = tm ?: export.paper.term,
                        examType = eType ?: export.paper.examType
                    )
                    export.copy(subject = updatedSub, paper = updatedPap)
                }

                JsonExporter.export(updatedExports, tempOutputDir)

                job.emit("Compressing output", "Creating ZIP package...", 96)
                val zipBytes = zipFolder(tempOutputDir)

                job.finish(zipBytes)
            } catch (e: ConversionError) {
                job.fail(e.message ?: "Unknown conversion error")
            } catch (e: Exception) {
                job.fail("Server Error: ${e.message}")
            } finally {
                pdf.delete()
                tempOutputDir?.deleteRecursively()
            }
        }

        call.respondText("""{"jobId":"${job.id}"}""", ContentType.Application.Json)
    }

    get("/api/progress/{jobId}") {
        val jobId = call.parameters["jobId"]
        val job = jobId?.let { JobRegistry.get(it) }
        if (job == null) {
            call.respond(HttpStatusCode.NotFound, "Job not found")
            return@get
        }

        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            val timeoutTime = System.currentTimeMillis() + 5 * 60 * 1000
            while (System.currentTimeMillis() < timeoutTime) {
                val event = job.events.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (event != null) {
                    val escapedStep = event.step.replace("\"", "\\\"")
                    val escapedDetail = event.detail.replace("\"", "\\\"").replace("\n", " ")
                    val json = """{"step":"$escapedStep","detail":"$escapedDetail","percent":${event.percent},"status":"${event.status}"}"""
                    write("data: $json\n\n")
                    flush()
                    if (event.status == JobStatus.DONE || event.status == JobStatus.ERROR) {
                        break
                    }
                } else if (job.status == JobStatus.DONE || job.status == JobStatus.ERROR) {
                    break
                }
            }
        }
    }

    get("/api/result/{jobId}") {
        val jobId = call.parameters["jobId"]
        val job = jobId?.let { JobRegistry.get(it) }
        if (job == null) {
            call.respondText("Job not found or expired.", status = HttpStatusCode.NotFound)
            return@get
        }

        when (job.status) {
            JobStatus.DONE -> {
                val bytes = job.resultZip
                if (bytes == null) {
                    call.respondText("Result not available.", status = HttpStatusCode.InternalServerError)
                    return@get
                }
                val zipName = "QuizOn_Export_${System.currentTimeMillis()}.zip"
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, zipName).toString()
                )
                call.respondBytes(bytes, ContentType.Application.Zip)
            }
            JobStatus.ERROR -> {
                call.respondText("Conversion Failed: ${job.errorMessage}", status = HttpStatusCode.UnprocessableEntity)
            }
            else -> {
                call.respondText("Job is still processing.", status = HttpStatusCode.Conflict)
            }
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
