plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

group = "net.vplaygames"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("net.vplaygames.quizonconvertor.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // PDF extraction
    implementation(libs.pdfbox)

    // JSON serialization
    implementation(libs.kotlinx.serialization.json)

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation(libs.logback)

    // Test
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}