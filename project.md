# QuizOnConvertor — LLM Context & Architecture Knowledge Base

> **System Designation**: LLM Context Document & Codebase Map  
> **Target Project**: `QuizOnConvertor`  
> **File Path**: `file:///d:/Projects/QuizOnConvertor/plan.md`  
> **Last Updated**: 2026-08-05

---

## 1. Project Identity

### Project Overview
- **Name**: `QuizOnConvertor`
- **Summary**: A high-performance, color-aware Kotlin JVM converter that parses AMRITA Computer-Based Exam (CBE) PDFs into structured QuizOn Django JSON data models and extracts embedded diagram images.
- **Intended Users**: QuizOn backend administrators, faculty, content creators, and automated exam ingestion pipelines.
- **Primary Goals**:
  1. Automate conversion of raw exam PDF documents into QuizOn-compatible JSON schemas.
  2. Reliably extract answer keys by inspecting PDF text stream RGB/CMYK color metadata (green `#008000` = correct answer, red `#FF0000` = incorrect).
  3. Extract inline question diagram images and option graphics, filtering out radio/checkbox UI icons.
  4. Provide both a flexible CLI tool (with `--dry-run` and metadata flags) and an aesthetic Ktor Web GUI application.

### Key Features
- **Color-Aware PDF Parsing**: Custom `PDFTextStripper` (`ColorTextStripper`) captures character coordinates and RGB fill colors directly from PDF graphics state streams.
- **State Machine Question Builder**: Tokenizes colored text lines, splits sections, and processes questions (MCQ, MSQ, NAT) with marks and option IDs.
- **Image Association Engine**: Extracts inline graphics, filters out UI artifacts ($\le 20\times 20\text{ px}$), and links images to questions or options based on page and $Y$-coordinate proximity.
- **Dual Interface**: Runs as a CLI executable or a lightweight Ktor 3.5.1 Netty web server with a glassmorphism drag-and-drop UI serving ZIP archives.
- **Validation Engine**: `JsonValidator` performs structural and domain rule checks before export.

---

## 2. Technology Stack

| Category | Technology | Role / Purpose |
|----------|------------|----------------|
| **Language** | Kotlin 2.4.10 | Primary language for strong typing, DSLs, and immutability |
| **Runtime** | JVM (JDK 25) | Execution environment and toolchain |
| **Build System** | Gradle 9.6.0 | Build automation, version catalog management (`libs.versions.toml`) |
| **PDF Processing** | Apache PDFBox 3.0.8 | PDF parsing, graphics stream operator listening, image extraction |
| **Serialization** | `kotlinx.serialization` 1.11.0 | JSON encoding/decoding for `QuizExport` data models |
| **Web Server** | Ktor 3.5.1 (Netty Engine) | Embedded JVM HTTP web server hosting GUI and conversion REST API |
| **Templating** | `kotlinx.html` DSL | Type-safe HTML rendering for the Web GUI upload page |
| **Logging** | Logback 1.6.1 | SLF4J logging framework backend |
| **Testing** | `kotlin.test` + JUnit 5 | Unit tests for parser, extractor, serializer, validator, and server |

---

## 3. Design System (Web GUI)

### Theme & Visual Philosophy
Modern dark mode with glassmorphism card elevation, vibrant accent gradients, and dynamic micro-interactions. Designed to provide an intuitive experience for upload and batch conversion.

### Color Palette
- **Background**: `#0f172a` (Deep Slate / Dark Mode Base)
- **Card Background**: `rgba(30, 41, 59, 0.7)` with `backdrop-filter: blur(16px)`
- **Accent Gradient**: `linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)` (Indigo → Purple → Pink)
- **Text Main**: `#f8fafc` (Slate 50)
- **Text Muted**: `#94a3b8` (Slate 400)
- **Success Color**: `#22c55e` (Emerald 500)
- **Border Color**: `rgba(255, 255, 255, 0.1)`

### Typography & UI Principles
- **Font Family**: `'Outfit'`, sans-serif (Google Fonts)
- **Border Radius**: Cards `24px`, Form inputs `10px`, Action buttons `12px`
- **UI Principles**: Drag-and-drop dropzone highlight states, smooth hover elevations (`transform: translateY(-2px)`), loading spinners, clear status messaging.

---

## 4. Architecture Overview

```
                          ┌──────────────────────────┐
                          │   Input PDF / Web / CLI  │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │  ColorTextStripper (P1)  │
                          │  (PDFBox Graphics Stream)│
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │   PageContent / Tokens   │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │  SectionSplitter &       │
                          │  QuestionBuilder (P2)    │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │ PdfImageExtractor (P3)   │
                          │ & ImageAssociator        │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │   JsonValidator (P6)     │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                          ┌──────────────────────────┐
                          │ JsonExporter / CLI / Web │
                          │ (JSON + Images + ZIP)    │
                          └──────────────────────────┘
```

### Layer Breakdown
1. **Extraction Layer (`extractor/`)**: Reads PDF text and graphics stream. `ColorTextStripper` records exact RGB fill colors per character line. `PdfImageExtractor` parses `PDImageXObject` resources.
2. **Parser Layer (`parser/`)**: `LineClassifier` categorizes lines (Section header, Question start, Option, Marks, ID). `SectionSplitter` isolates multi-section papers. `QuestionBuilder` state machine builds questions and maps green/red text color to `isCorrect`. `ImageAssociator` maps extracted images to questions/options by page/$Y$ coordinates.
3. **Domain Layer (`model/`)**: Immutable Kotlin data classes (`QuizExport`, `SubjectData`, `QuizPaperData`, `QuestionData`, `OptionData`).
4. **Validation Layer (`validation/`)**: `JsonValidator` verifies model consistency, valid question types, option existence, and NAT numerical answer presence.
5. **Output / Presentation Layer (`serializer/`, `server/`, `Main.kt`)**: `JsonExporter` writes formatted JSON and `conversion_report.txt`. `Routes.kt` handles Web GUI requests and streams ZIP downloads.

---

## 5. Folder Structure

```
QuizOnConvertor/
├── build.gradle.kts                              # Gradle build script with Ktor, PDFBox, Serialization
├── settings.gradle.kts                            # Project settings
├── gradle/libs.versions.toml                      # Version catalog
├── README.md                                      # Public repository user guide
├── plan.md                                        # THIS FILE: Comprehensive LLM Context & Knowledge Base
├── Sem1 Maths1.pdf                                # Specimen AMRITA CBE exam PDF
│
├── src/main/kotlin/net/vplaygames/quizonconvertor/
│   ├── Main.kt                                    # Main entry point (CLI parsing & --server mode switch)
│   ├── model/
│   │   ├── QuizExport.kt                          # Top-level export container schema
│   │   ├── SubjectData.kt                         # Subject metadata model
│   │   ├── QuizPaperData.kt                       # Paper metadata model
│   │   ├── QuestionData.kt                        # Question and Option models
│   │   └── TagData.kt                             # Reference tag model
│   ├── extractor/
│   │   ├── ColorTextStripper.kt                   # Custom PDFTextStripper recording character fill colors
│   │   ├── PdfTextExtractor.kt                    # Orchestrates page-by-page text & color extraction
│   │   ├── PdfImageExtractor.kt                   # PDFBox image stream extractor
│   │   └── PageContent.kt                         # Data structures for ColoredLine & PageContent
│   ├── parser/
│   │   ├── LineClassifier.kt                      # Token classification logic
│   │   ├── SectionSplitter.kt                     # Splits document content by subject section headers
│   │   ├── QuestionBuilder.kt                     # Core state machine building questions & option keys
│   │   ├── ImageAssociator.kt                     # Maps extracted images to questions/options by Y-coords
│   │   ├── PdfParser.kt                           # Top-level parsing orchestrator
│   │   └── ConversionError.kt                     # Custom domain exception type
│   ├── serializer/
│   │   └── JsonExporter.kt                        # Writes JSON files and conversion_report.txt
│   ├── server/
│   │   ├── Server.kt                              # Ktor Netty server initializer
│   │   ├── Routes.kt                              # Web UI, health endpoint, & ZIP convert endpoint
│   │   └── Pages.kt                              # kotlinx.html HTML DSL view template
│   └── validation/
│       └── JsonValidator.kt                       # Domain validation and diagnostic rules
│
└── src/test/kotlin/net/vplaygames/quizonconvertor/
    ├── extractor/
    │   └── PdfTextExtractorTest.kt                # Extractor unit tests
    ├── parser/
    │   ├── QuestionBuilderTest.kt                 # Parser state machine unit tests
    │   └── ImageAssociatorTest.kt                 # Image association unit tests
    ├── serializer/
    │   └── JsonExporterTest.kt                    # JSON exporter unit tests
    ├── server/
    │   └── ServerTest.kt                          # Ktor server route & health tests
    └── validation/
        └── ValidationTest.kt                      # Domain validator & specimen accuracy tests
```

---

## 6. Module Documentation

### Module `net.vplaygames.quizonconvertor.extractor`
- **Purpose**: Low-level PDF graphics stream and text extraction.
- **Key Files**:
  - [ColorTextStripper.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/extractor/ColorTextStripper.kt): Overrides `processTextPosition()` in PDFBox `PDFTextStripper` to capture current `graphicsState.nonStrokingColor`.
  - [PdfTextExtractor.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/extractor/PdfTextExtractor.kt): Processes PDF file page by page into `List<PageContent>`.
  - [PdfImageExtractor.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/extractor/PdfImageExtractor.kt): Listens to `DrawObject` operators to extract inline raster images (`ExtractedImage`) with coordinate bounds $(x, y, w, h)$.

### Module `net.vplaygames.quizonconvertor.parser`
- **Purpose**: Converts raw colored text lines into structured domain objects.
- **Key Files**:
  - [LineClassifier.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/parser/LineClassifier.kt): Regular expression token matching for question IDs, option labels (`Option 1 :`), marks `[4]`, section titles, and NAT text.
  - [SectionSplitter.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/parser/SectionSplitter.kt): Splits full document lines into per-subject sections based on section header lines.
  - [QuestionBuilder.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/parser/QuestionBuilder.kt): State machine processing tokens line-by-line. Decodes green text (`#008000`) as `isCorrect = true` and red text (`#FF0000`) as `isCorrect = false`. Classifies question types (`mcq`, `msq`, `nat`).
  - [ImageAssociator.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/parser/ImageAssociator.kt): Filters UI icons ($\le 20\text{px}$) and maps content images to question headers or options by page/$Y$-coordinate proximity.
  - [PdfParser.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/parser/PdfParser.kt): Top-level orchestrator calling extraction, section splitting, question building, and image association.

### Module `net.vplaygames.quizonconvertor.serializer`
- **Purpose**: Output generation.
- **Key Files**:
  - [JsonExporter.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/serializer/JsonExporter.kt): Encodes `QuizExport` to formatted `{section_name}.json` and writes `conversion_report.txt`.

### Module `net.vplaygames.quizonconvertor.server`
- **Purpose**: Browser web interface and REST API.
- **Key Files**:
  - [Server.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Server.kt): Embedded Netty server setup.
  - [Routes.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Routes.kt): Web handlers for `GET /`, `GET /api/health`, and `POST /api/convert` (multipart upload & ZIP download).
  - [Pages.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Pages.kt): HTML DSL template for the web UI.

### Module `net.vplaygames.quizonconvertor.validation`
- **Purpose**: Validation and diagnostics.
- **Key Files**:
  - [JsonValidator.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/validation/JsonValidator.kt): Validates structural schema rules and generates warning/error diagnostics.

---

## 7. Route Documentation

### `GET /`
- **Purpose**: Serves the single-page drag-and-drop web UI.
- **Content Type**: `text/html`
- **Features**: PDF dropzone, metadata inputs (subject code, year, term, exam type), submit handler.

### `GET /api/health`
- **Purpose**: Health check endpoint for container probes or monitoring.
- **Response**: `{"status": "ok"}` (`application/json`)

### `POST /api/convert`
- **Purpose**: Converts uploaded PDF into JSON models & images and streams back a ZIP archive.
- **Content Type**: `multipart/form-data`
- **Form Fields**:
  - `file`: PDF file stream
  - `subjectCode`: Optional subject code override string
  - `year`: Optional exam year integer
  - `term`: Optional term string (`jan`, `may`, `sept`)
  - `examType`: Optional exam type string (`quiz1`, `quiz2`, `endterm`)
  - `strict`: Optional boolean (`true` / `false`)
- **Response**: `application/zip` containing `{section}.json`, `images/` directory, and `conversion_report.txt`.

---

## 8. Component Catalog (Web GUI & Server)

- **`renderIndexPage()`** ([Pages.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Pages.kt)): Type-safe HTML DSL template producing the application container, header, upload box, form grid, and upload script.
- **`configureRoutes()`** ([Routes.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Routes.kt)): Configures Ktor routing handlers and memory zip streaming pipeline.
- **`zipFolder()`** ([Routes.kt](file:///d:/Projects/QuizOnConvertor/src/main/kotlin/net/vplaygames/quizonconvertor/server/Routes.kt)): Compresses output folders into a `ByteArray` zip payload.

---

## 9. Data Models

### `QuizExport`
Top-level JSON export structure container.
```kotlin
@Serializable
data class QuizExport(
    val subject: SubjectData,
    val paper: QuizPaperData,
    val questions: List<QuestionData>
)
```

### `SubjectData`
```kotlin
@Serializable
data class SubjectData(
    val subject: String,
    val code: String = "",
    val level: String = "Foundation",
    val description: String = "",
    val icon: String = ""
)
```

### `QuizPaperData`
```kotlin
@Serializable
data class QuizPaperData(
    val title: String,
    val year: Int = 2025,
    val term: String = "may",
    val examType: String = "endterm",
    val totalDurationSeconds: Int = 0,
    val isPublished: Boolean = false
)
```

### `QuestionData` & `OptionData`
```kotlin
@Serializable
data class QuestionData(
    val text: String,
    val qType: String, // "mcq" | "msq" | "nat"
    val order: Int,
    val image: String? = null,
    val correctAnswer: String? = null,
    val explanation: String = "",
    val marks: Int = 4,
    val negativeMarks: Int = 0,
    val codeSnippet: String? = null,
    val natTolerance: Double? = null,
    val referenceTags: List<TagData> = emptyList(),
    val options: List<OptionData> = emptyList(),
    val sourceQuestionId: String? = null,
    val sourceQuestionNumber: Int? = null,
    val comprehensionParentId: String? = null
)

@Serializable
data class OptionData(
    val serial: Int,
    val text: String,
    val image: String? = null,
    val isCorrect: Boolean = false,
    val sourceOptionId: String? = null
)
```

---

## 10. API Documentation

### `POST /api/convert`

**Request Example**:
`multipart/form-data` upload containing PDF file `Sem1 Maths1.pdf`.

**Successful Response**:
`200 OK`
Headers:
`Content-Disposition: attachment; filename="QuizOn_Export_1722867600.zip"`
`Content-Type: application/zip`

ZIP Archive Structure:
```
QuizOn_Export.zip
├── Sem1_Maths1.json
├── conversion_report.txt
└── images/
    └── Sem1_Maths1/
        ├── q2_img.png
        └── opt_6406533039095.png
```

**Error Responses**:
- `400 Bad Request`: No PDF file provided.
- `422 Unprocessable Entity`: PDF parsing failed or document malformed (`ConversionError`).
- `500 Internal Server Error`: Unexpected server processing exception.

---

## 11. State Management

- **QuestionBuilder State Machine**:
  - `IDLE` → `HEADER` (accumulates question text) → `OPTIONS` (accumulates option items) → `NAT_ANSWER` (accumulates numerical answer) → `FLUSH` (creates `QuestionData` and resets state).
- **CLI Options State**: Immutable `CliOptions` data class populated via `parseCliArgs(args)`.
- **Web Server State**: Stateless per request; temporary PDF files and conversion outputs created in system `tmp` directories are cleaned up in `finally` blocks.

---

## 12. Business Logic Workflows

```
PDF Document Input
       │
       ▼
Extract Colored Lines (RGB/CMYK) ──▶ Extracted Characters + Coordinates
       │
       ▼
Split Content by Sections ──────────▶ Per-Subject Line Collections
       │
       ▼
Tokenize & Classify Lines ─────────▶ Question Headers, Options, Marks, IDs
       │
       ▼
Build Question Data ────────────────▶ Determine qType (MCQ/MSQ/NAT) & isCorrect (Green/Red)
       │
       ▼
Extract & Associate Images ────────▶ Filter UI icons (≤20px) & Map Diagrams by Y-coords
       │
       ▼
Run JsonValidator ─────────────────▶ Verify Schema Rules & Record Diagnostics
       │
       ▼
JsonExporter Output ───────────────▶ Write {section}.json & conversion_report.txt (or ZIP)
```

---

## 13. External Integrations

- **Apache PDFBox 3.0.4**: Internal graphics operator listening, PDF stream processing, image extraction.
- **QuizOn Django Backend**: Consumes produced `{section}.json` files via management commands, Django fixtures, or API POST endpoints.

---

## 14. Development Workflow

```bash
# 1. Build project and run test suite
./gradlew test

# 2. Run CLI conversion on specimen PDF
./gradlew run --args="'Sem1 Maths1.pdf' --output ./output --subject-code MAT101 --verbose"

# 3. Run Dry-Run CLI validation
./gradlew run --args="'Sem1 Maths1.pdf' --dry-run"

# 4. Start Ktor Web GUI server
./gradlew run --args="--server --port 8080"

# 5. Build executable JAR
./gradlew jar
```

---

## 15. Coding Conventions

- **Kotlin Style**: Standard Kotlin coding conventions. Use immutability (`val`) wherever possible.
- **Sealed & Data Classes**: Prefer Kotlin `data class` for data holders and models.
- **Color Decoding Rules**:
  - RGB Green (`r < 50, g > 100, b < 50` or `#008000`) $\rightarrow$ `isCorrect = true`
  - RGB Red (`r > 180, g < 50, b < 50` or `#FF0000`) $\rightarrow$ `isCorrect = false`
- **Error Handling**: Use domain-specific `ConversionError` exceptions for expected PDF layout malformations. Avoid swallowing exceptions without logging.

---

## 16. Project Rules

1. **Preserve Relative Image Paths**: Always reference extracted images with relative paths (`images/{section}/q{order}_img.png`) in `QuestionData.image` and `OptionData.image`.
2. **Never Ignore UI Icon Filtering**: Small icons ($\le 20\times 20\text{ px}$) MUST be filtered out during image extraction to avoid treating radio button/checkbox UI elements as question content images.
3. **Keep Main CLI and Server Separate**: `Main.kt` delegates CLI execution to `runCli()` logic and web execution to `startServer()`.
4. **Mandatory Verification**: Every major parser change MUST be validated by running `./gradlew test`.

---

## 17. Known TODOs & Future Roadmap

- [ ] Add OCR fallback for scanned PDF documents lacking native text streams.
- [ ] Add batch CLI processing flag for entire directories containing multiple PDF papers.
- [ ] Add direct Django API push integration (`POST /api/v1/quiz-papers/import/`).

---

## 18. Glossary

- **AMRITA CBE**: Computer-Based Exam platform utilized by Amrita Vishwa Vidyapeetham.
- **MCQ**: Multiple Choice Question (single correct option).
- **MSQ**: Multiple Select Question (one or more correct options).
- **NAT**: Numerical Answer Type (numerical input answer with optional tolerance).
- **Colorspace**: PDF color representation mode (`CalRGB`, `DeviceRGB`, `DeviceCMYK`, `DeviceGray`).
- **ColoredLine**: A line of extracted PDF text associated with its dominant RGB fill color and page line number.
- **QuizExport**: Top-level JSON data container matching the QuizOn Django backend paper import contract.

---

## 19. Quick Navigation Guide

- **PDF Text & Color Stripper**: `src/main/kotlin/net/vplaygames/quizonconvertor/extractor/ColorTextStripper.kt`
- **Question State Machine**: `src/main/kotlin/net/vplaygames/quizonconvertor/parser/QuestionBuilder.kt`
- **Image Associator**: `src/main/kotlin/net/vplaygames/quizonconvertor/parser/ImageAssociator.kt`
- **JSON Exporter**: `src/main/kotlin/net/vplaygames/quizonconvertor/serializer/JsonExporter.kt`
- **Web Routes**: `src/main/kotlin/net/vplaygames/quizonconvertor/server/Routes.kt`
- **HTML GUI DSL**: `src/main/kotlin/net/vplaygames/quizonconvertor/server/Pages.kt`
- **Domain Validator**: `src/main/kotlin/net/vplaygames/quizonconvertor/validation/JsonValidator.kt`
- **Main CLI & Server Entry**: `src/main/kotlin/net/vplaygames/quizonconvertor/Main.kt`

---

## 20. Agent Instructions

When modifying or extending `QuizOnConvertor`:
- **Read before modifying**: Inspect target files completely using `view_file`.
- **Preserve Color Logic**: Never alter RGB color thresholding without validating against `Sem1 Maths1.pdf` specimen test cases.
- **Verify Build**: Always run `./gradlew test` after code changes to confirm no regressions.
- **Update Documentation**: Keep `README.md` and this context document (`plan.md`) synchronized whenever adding new CLI options, models, or server routes.
