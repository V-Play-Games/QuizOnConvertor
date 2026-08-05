package net.vplaygames.quizonconvertor

import net.vplaygames.quizonconvertor.parser.ConversionError
import net.vplaygames.quizonconvertor.parser.PdfParser
import net.vplaygames.quizonconvertor.serializer.JsonExporter
import net.vplaygames.quizonconvertor.server.startServer
import net.vplaygames.quizonconvertor.validation.JsonValidator
import java.io.File

data class CliOptions(
    var serverMode: Boolean = false,
    var port: Int = 8080,
    var pdfPath: String = "Sem1 Maths1.pdf",
    var outputDir: File = File("output"),
    var imagesDir: File? = null,
    var subjectCode: String? = null,
    var year: Int? = null,
    var term: String? = null,
    var examType: String? = null,
    var pretty: Boolean = true,
    var verbose: Boolean = false,
    var strict: Boolean = false,
    var dryRun: Boolean = false
)

fun parseCliArgs(args: Array<String>): CliOptions {
    val options = CliOptions()
    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--server", "-s" -> {
                options.serverMode = true
            }
            "--port" -> {
                if (i + 1 < args.size) options.port = args[++i].toIntOrNull() ?: 8080
            }
            "--output", "-o" -> {
                if (i + 1 < args.size) options.outputDir = File(args[++i])
            }
            "--images-dir" -> {
                if (i + 1 < args.size) options.imagesDir = File(args[++i])
            }
            "--subject-code" -> {
                if (i + 1 < args.size) options.subjectCode = args[++i]
            }
            "--year" -> {
                if (i + 1 < args.size) options.year = args[++i].toIntOrNull()
            }
            "--term" -> {
                if (i + 1 < args.size) options.term = args[++i]
            }
            "--exam-type" -> {
                if (i + 1 < args.size) options.examType = args[++i]
            }
            "--pretty" -> {
                options.pretty = true
            }
            "--no-pretty" -> {
                options.pretty = false
            }
            "--verbose" -> {
                options.verbose = true
            }
            "--strict" -> {
                options.strict = true
            }
            "--dry-run" -> {
                options.dryRun = true
            }
            else -> {
                if (!arg.startsWith("-")) {
                    options.pdfPath = arg
                }
            }
        }
        i++
    }
    return options
}

fun main(args: Array<String>) {
    val cliOptions = parseCliArgs(args)

    if (cliOptions.serverMode) {
        startServer(port = cliOptions.port, wait = true)
        return
    }

    val pdfFile = File(cliOptions.pdfPath)

    if (!pdfFile.exists()) {
        println("Error: File not found at ${pdfFile.absolutePath}")
        println("\nUsage: QuizOnConvertor <pdf-file> [options]")
        println("\nOptions:")
        println("  --output, -o <dir>         Output directory (default: ./output)")
        println("  --images-dir <dir>         Directory for extracted images (default: <output>/images)")
        println("  --subject-code <code>      Subject code (e.g., MAT101)")
        println("  --year <year>              Exam year")
        println("  --term <term>              Term: jan | may | sept")
        println("  --exam-type <type>         Type: quiz1 | quiz2 | endterm")
        println("  --pretty                   Pretty-print JSON (default: true)")
        println("  --verbose                  Print extraction details")
        println("  --strict                   Fail on any warnings (default: false)")
        println("  --dry-run                  Simulate parsing without writing files")
        println("  --server, -s               Launch Ktor Web GUI server mode")
        return
    }

    println("Parsing PDF document: ${pdfFile.absolutePath}")
    try {
        val parsedExports = PdfParser.parse(
            pdfFile = pdfFile,
            outputDir = if (cliOptions.dryRun) File(System.getProperty("java.io.tmpdir")) else (cliOptions.imagesDir?.parentFile ?: cliOptions.outputDir),
            strict = cliOptions.strict
        )

        // Apply CLI metadata overrides if specified
        val updatedExports = parsedExports.map { export ->
            val updatedSubject = if (cliOptions.subjectCode != null) {
                export.subject.copy(code = cliOptions.subjectCode!!)
            } else export.subject

            val updatedPaper = export.paper.copy(
                year = cliOptions.year ?: export.paper.year,
                term = cliOptions.term ?: export.paper.term,
                examType = cliOptions.examType ?: export.paper.examType
            )

            export.copy(subject = updatedSubject, paper = updatedPaper)
        }

        if (cliOptions.dryRun) {
            println("\n=== QuizOnConvertor Dry Run Summary ===")
            println("Input PDF: ${pdfFile.absolutePath}")
            println("Parsed Sections: ${updatedExports.size}")
            
            var totalQ = 0
            var totalWarnings = 0
            var totalErrors = 0

            updatedExports.forEach { export ->
                val valResult = JsonValidator.validate(export)
                totalQ += export.questions.size
                totalWarnings += valResult.warnings.size
                totalErrors += valResult.errors.size
                
                println("\nSection: ${export.paper.title}")
                println("  Subject: ${export.subject.subject} (Code: ${export.subject.code})")
                println("  Questions: ${export.questions.size} (MCQ: ${export.questions.count { it.qType == "mcq" }}, MSQ: ${export.questions.count { it.qType == "msq" }}, NAT: ${export.questions.count { it.qType == "nat" }})")
                if (valResult.errors.isNotEmpty()) {
                    println("  Errors (${valResult.errors.size}):")
                    valResult.errors.forEach { err -> println("    - $err") }
                }
                if (valResult.warnings.isNotEmpty()) {
                    println("  Warnings (${valResult.warnings.size}):")
                    valResult.warnings.forEach { warn -> println("    - $warn") }
                }
            }

            println("\n[DRY RUN] Complete. Processed $totalQ questions across ${updatedExports.size} section(s). 0 files written to output.")
            return
        }

        // Export per-section JSON files and conversion_report.txt
        val generatedFiles = JsonExporter.export(
            exports = updatedExports,
            outputDir = cliOptions.outputDir,
            prettyPrint = cliOptions.pretty
        )

        println("\n=== QuizOnConvertor CLI Pipeline Complete ===")
        println("Output Directory: ${cliOptions.outputDir.absolutePath}")
        println("Generated Files (${generatedFiles.size}):")
        generatedFiles.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
        }

        if (cliOptions.verbose) {
            updatedExports.forEachIndexed { idx, export ->
                println("\n--- Section ${idx + 1}: ${export.paper.title} ---")
                println("Subject: ${export.subject.subject} (Code: ${export.subject.code}, Level: ${export.subject.level})")
                println("Total Questions: ${export.questions.size}")
                val mcq = export.questions.count { it.qType == "mcq" }
                val msq = export.questions.count { it.qType == "msq" }
                val nat = export.questions.count { it.qType == "nat" }
                println("  - MCQ: $mcq, MSQ: $msq, NAT: $nat")
            }
        }
    } catch (e: ConversionError) {
        println("Conversion Error: ${e.message}")
    } catch (e: Exception) {
        println("Unexpected Error during conversion: ${e.message}")
        e.printStackTrace()
    }
}
