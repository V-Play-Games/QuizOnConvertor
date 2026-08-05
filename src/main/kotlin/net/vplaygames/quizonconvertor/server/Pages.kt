package net.vplaygames.quizonconvertor.server

import kotlinx.html.*

fun HTML.renderIndexPage() {
    head {
        title("QuizOnConvertor — Exam PDF to Django Quiz Model Converter")
        meta { charset = "UTF-8" }
        meta { name = "viewport"; content = "width=device-width, initial-scale=1.0" }
        link {
            rel = "stylesheet"
            href = "https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap"
        }
        style {
            unsafe {
                +"""
                :root {
                    --bg-dark: #0f172a;
                    --card-bg: rgba(30, 41, 59, 0.7);
                    --accent-gradient: linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%);
                    --accent-hover: linear-gradient(135deg, #4f46e5 0%, #9333ea 50%, #db2777 100%);
                    --text-main: #f8fafc;
                    --text-muted: #94a3b8;
                    --border-color: rgba(255, 255, 255, 0.1);
                    --success-color: #22c55e;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: 'Outfit', sans-serif;
                    background: var(--bg-dark);
                    color: var(--text-main);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    padding: 2rem;
                    background-image: 
                        radial-gradient(at 0% 0%, rgba(99, 102, 241, 0.15) 0px, transparent 50%),
                        radial-gradient(at 100% 100%, rgba(236, 72, 153, 0.15) 0px, transparent 50%);
                }
                .container {
                    width: 100%;
                    max-width: 800px;
                    background: var(--card-bg);
                    backdrop-filter: blur(16px);
                    border: 1px solid var(--border-color);
                    border-radius: 24px;
                    padding: 2.5rem;
                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
                }
                .header {
                    text-align: center;
                    margin-bottom: 2rem;
                }
                .header h1 {
                    font-size: 2.5rem;
                    font-weight: 700;
                    background: var(--accent-gradient);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    margin-bottom: 0.5rem;
                }
                .header p {
                    color: var(--text-muted);
                    font-size: 1.05rem;
                }
                .upload-box {
                    border: 2px dashed var(--border-color);
                    border-radius: 16px;
                    padding: 3rem 1.5rem;
                    text-align: center;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    background: rgba(15, 23, 42, 0.4);
                    margin-bottom: 1.5rem;
                    position: relative;
                }
                .upload-box:hover, .upload-box.dragover {
                    border-color: #a855f7;
                    background: rgba(168, 85, 247, 0.08);
                }
                .upload-icon {
                    font-size: 3rem;
                    margin-bottom: 1rem;
                }
                .file-input {
                    position: absolute;
                    top: 0; left: 0; width: 100%; height: 100%;
                    opacity: 0; cursor: pointer;
                }
                .file-info {
                    margin-top: 0.5rem;
                    font-weight: 600;
                    color: var(--success-color);
                }
                .form-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 1.25rem;
                    margin-bottom: 2rem;
                }
                .form-group {
                    display: flex;
                    flex-direction: column;
                    gap: 0.4rem;
                }
                label {
                    font-size: 0.9rem;
                    font-weight: 500;
                    color: var(--text-muted);
                }
                input, select {
                    background: rgba(15, 23, 42, 0.6);
                    border: 1px solid var(--border-color);
                    border-radius: 10px;
                    padding: 0.75rem 1rem;
                    color: var(--text-main);
                    font-family: inherit;
                    font-size: 1rem;
                    outline: none;
                    transition: border-color 0.2s;
                }
                input:focus, select:focus {
                    border-color: #a855f7;
                }
                .btn-submit {
                    width: 100%;
                    padding: 1rem;
                    background: var(--accent-gradient);
                    border: none;
                    border-radius: 12px;
                    color: white;
                    font-family: inherit;
                    font-size: 1.1rem;
                    font-weight: 600;
                    cursor: pointer;
                    transition: transform 0.2s, box-shadow 0.2s;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 0.5rem;
                }
                .btn-submit:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 10px 25px -5px rgba(168, 85, 247, 0.4);
                }
                .spinner {
                    border: 3px solid rgba(255,255,255,0.3);
                    border-radius: 50%;
                    border-top: 3px solid white;
                    width: 20px; height: 20px;
                    animation: spin 1s linear infinite;
                    display: none;
                }
                @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                """.trimIndent()
            }
        }
    }
    body {
        div("container") {
            div("header") {
                h1 { +"QuizOn PDF Converter" }
                p { +"Extract color-encoded exam PDFs into QuizOn JSON models & images" }
            }
            form(action = "/api/convert", method = FormMethod.post, encType = FormEncType.multipartFormData) {
                id = "convertForm"

                div("upload-box") {
                    id = "uploadBox"
                    div("upload-icon") { +"📄" }
                    div("upload-text") { +"Drag & drop AMRITA exam PDF here or click to browse" }
                    div("file-info") { id = "fileInfo" }
                    input(type = InputType.file, name = "file") {
                        id = "pdfFileInput"
                        classes = setOf("file-input")
                        accept = ".pdf"
                        required = true
                    }
                }

                div("form-grid") {
                    div("form-group") {
                        label { +"Subject Code (Optional)" }
                        input(type = InputType.text, name = "subjectCode") {
                            placeholder = "e.g. MAT101"
                        }
                    }
                    div("form-group") {
                        label { +"Exam Year" }
                        input(type = InputType.number, name = "year") {
                            value = "2025"
                        }
                    }
                    div("form-group") {
                        label { +"Term" }
                        select {
                            name = "term"
                            option { value = "may"; selected = true; +"May" }
                            option { value = "jan"; +"January" }
                            option { value = "sept"; +"September" }
                        }
                    }
                    div("form-group") {
                        label { +"Exam Type" }
                        select {
                            name = "examType"
                            option { value = "endterm"; selected = true; +"Endterm" }
                            option { value = "quiz1"; +"Quiz 1" }
                            option { value = "quiz2"; +"Quiz 2" }
                        }
                    }
                }

                button(type = ButtonType.submit, classes = "btn-submit") {
                    id = "submitBtn"
                    span { id = "btnText"; +"Convert PDF to ZIP" }
                    div("spinner") { id = "btnSpinner" }
                }
            }

            script {
                unsafe {
                    +"""
                    const pdfInput = document.getElementById('pdfFileInput');
                    const fileInfo = document.getElementById('fileInfo');
                    const uploadBox = document.getElementById('uploadBox');

                    pdfInput.addEventListener('change', (e) => {
                        if (e.target.files.length > 0) {
                            fileInfo.textContent = 'Selected: ' + e.target.files[0].name;
                        }
                    });

                    ['dragenter', 'dragover'].forEach(eventName => {
                        uploadBox.addEventListener(eventName, (e) => { e.preventDefault(); uploadBox.classList.add('dragover'); }, false);
                    });
                    ['dragleave', 'drop'].forEach(eventName => {
                        uploadBox.addEventListener(eventName, (e) => { e.preventDefault(); uploadBox.classList.remove('dragover'); }, false);
                    });
                    uploadBox.addEventListener('drop', (e) => {
                        const dt = e.dataTransfer;
                        const files = dt.files;
                        if (files.length > 0) {
                            pdfInput.files = files;
                            fileInfo.textContent = 'Selected: ' + files[0].name;
                        }
                    });
                    """.trimIndent()
                }
            }
        }
    }
}
