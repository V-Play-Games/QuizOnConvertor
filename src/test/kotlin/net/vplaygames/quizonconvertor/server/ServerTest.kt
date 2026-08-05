package net.vplaygames.quizonconvertor.server

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerTest {

    @Test
    fun testServerHealthAndIndexRoutes() {
        val port = 8099
        val serverEngine = startServer(port = port, wait = false)

        try {
            val client = HttpClient.newHttpClient()

            // 1. Test /api/health
            val healthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/api/health"))
                .GET()
                .build()
            val healthResp = client.send(healthReq, HttpResponse.BodyHandlers.ofString())

            assertEquals(200, healthResp.statusCode())
            assertEquals("""{"status":"ok"}""", healthResp.body())

            // 2. Test / (Index Page)
            val indexReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/"))
                .GET()
                .build()
            val indexResp = client.send(indexReq, HttpResponse.BodyHandlers.ofString())

            assertEquals(200, indexResp.statusCode())
            assertTrue(indexResp.body().contains("QuizOn PDF Converter"))
        } finally {
            serverEngine.stop(1000, 2000)
        }
    }
}
