# QuizOnConvertor 🎯

> **Color-Aware Exam PDF to QuizOn Django JSON Data Model Converter**

QuizOnConvertor is a high-performance Kotlin JVM library, CLI tool, and Ktor Web application that extracts structured question papers, options, answer keys, and embedded diagram images from AMRITA Computer-Based Exam PDFs.

---

## 💡 Key Differentiators

- **🎨 Color-Aware PDF Extraction**: Extracted RGB/CMYK color tokens directly determine answer keys (e.g. green fill `#008000` = correct answer, red `#FF0000` = incorrect).
- **🖼️ Embedded Image & Diagram Associator**: Automatically extracts question diagrams and option graphics, filters UI icons ($\le 20\times 20\text{ px}$), and links images to questions and options by page and $Y$-position coordinates.
- **⚡ Multiple Execution Modes**:
  - **CLI Pipeline**: Fully configurable command-line interface with `--dry-run`, `--strict`, and metadata flags.
  - **Ktor Web GUI**: Modern browser application with glassmorphism UI, drag-and-drop PDF upload, and instant ZIP archive download.
- **📊 Comprehensive Reports & Validation**: Includes `JsonValidator` for structural checks and generates `conversion_report.txt` detailing section stats, answer key completeness, and diagnostics.

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.1.20 (JVM Toolchain 25)
- **PDF Engine**: Apache PDFBox 3.0.4
- **Serialization**: kotlinx.serialization (JSON)
- **Web Engine**: Ktor 3.5.1 Server (Netty Engine) + kotlinx.html DSL
- **Build System**: Gradle 9.6.0 with Version Catalogs

---

## 🚀 Quick Start & Usage

### 1. Command Line Interface (CLI)

Run the CLI pipeline using Gradle:

```bash
# Basic conversion
./gradlew run --args="'Sem1 Maths1.pdf' --output ./output"

# Conversion with metadata overrides and verbose logging
./gradlew run --args="'Sem1 Maths1.pdf' --output ./output --subject-code MAT101 --year 2025 --term may --exam-type endterm --verbose"

# Dry-run mode (validate without writing files to disk)
./gradlew run --args="'Sem1 Maths1.pdf' --dry-run"
```

#### CLI Options Reference

| Option | Description | Default |
|--------|-------------|---------|
| `<pdf-file>` | Path to the target exam PDF file | `Sem1 Maths1.pdf` |
| `--output, -o <dir>` | Directory to save JSON exports and reports | `./output` |
| `--images-dir <dir>` | Custom output directory for extracted images | `<output>/images` |
| `--subject-code <code>` | Subject code override (e.g. `MAT101`) | Extracted / empty |
| `--year <year>` | Exam year override | Extracted / `2025` |
| `--term <term>` | Exam term (`jan`, `may`, `sept`) | `may` |
| `--exam-type <type>` | Exam type (`quiz1`, `quiz2`, `endterm`) | `endterm` |
| `--pretty` | Pretty-print output JSON files | `true` |
| `--verbose` | Print detailed section breakdown | `false` |
| `--strict` | Fail pipeline on any extraction warnings | `false` |
| `--dry-run` | Run parsing and validation without writing files | `false` |
| `--server, -s` | Launch Ktor Web GUI server mode | `false` |
| `--port <port>` | Port for Ktor server mode | `8080` |

---

### 2. Ktor Web GUI Server

To launch the browser-based conversion server:

```bash
./gradlew run --args="--server --port 8080"
```

Open your browser at **`http://localhost:8080`**:
- Drag and drop your exam PDF.
- Fill optional metadata fields.
- Click **"Convert PDF to ZIP"** to receive a structured `.zip` package containing all section JSONs, images, and conversion reports.

---

## 📄 JSON Export Schema

Each section is exported as `{section_name}.json`:

```json
{
  "subject": {
    "subject": "MATHEMATICS",
    "code": "MAT101",
    "level": "Foundation",
    "description": "Extracted from PDF section Sem1 Maths1",
    "icon": ""
  },
  "paper": {
    "title": "Sem1 Maths1",
    "year": 2025,
    "term": "may",
    "examType": "endterm",
    "totalDurationSeconds": 0,
    "isPublished": false
  },
  "questions": [
    {
      "text": "Which of the following is (are) correct?",
      "qType": "msq",
      "order": 1,
      "image": "images/Sem1_Maths1/q1_img.png",
      "correctAnswer": null,
      "explanation": "",
      "marks": 4,
      "negativeMarks": 0,
      "codeSnippet": null,
      "natTolerance": null,
      "referenceTags": [],
      "options": [
        {
          "serial": 1,
          "text": "Floyd-Warshall algorithm is used for all pair shortest paths.",
          "image": null,
          "isCorrect": true,
          "sourceOptionId": "6406533039095"
        }
      ],
      "sourceQuestionId": "640653902325",
      "sourceQuestionNumber": 1
    }
  ]
}
```

---

## 🧪 Testing

Run the test suite:

```bash
./gradlew test
```

### Test Coverage Highlights:
- `PdfTextExtractorTest`: Tests color token extraction.
- `QuestionBuilderTest`: Tests tokenizer, section splitting, and QuestionBuilder state machine logic.
- `ImageAssociatorTest`: Tests inline image coordinate association and icon filtering.
- `JsonExporterTest`: Tests JSON formatting and conversion report creation.
- `ServerTest`: Tests Ktor Netty server routes and `/api/health`.
- `ValidationTest`: Tests domain model validation rules and specimen answer key extraction accuracy.

---

## 🏛️ System Architecture

```
QuizOnConvertor/
├── src/main/kotlin/net/vplaygames/quizonconvertor/
│   ├── Main.kt                    # Entry point (CLI & Web server launcher)
│   ├── model/                     # Data models (QuizExport, SubjectData, QuestionData)
│   ├── extractor/                 # PDFTextStripper, color extractor, image engine
│   ├── parser/                    # Tokenizer, line classifier, QuestionBuilder state machine
│   ├── serializer/                # JsonExporter & conversion report writer
│   ├── server/                    # Ktor server engine, routes, & kotlinx.html UI
│   └── validation/                # JsonValidator validation rules
```

---

## 📝 License

Internal QuizOn Utility Project — V-Play Games. All rights reserved.
