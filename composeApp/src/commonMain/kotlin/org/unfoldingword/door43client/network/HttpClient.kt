package org.unfoldingword.door43client.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get

/**
 * HTTP client using Ktor for network requests.
 * Uses CIO engine which works on all platforms.
 */
class Door43HttpClient {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }
    
    /**
     * Performs a GET request and returns the response as a String.
     */
    suspend fun get(url: String): String = client.get(url).body()
    
    /**
     * Performs a GET request and deserializes the response to the specified type.
     */
    suspend inline fun <reified T> get(url: String): T = client.get(url).body()
    
    /**
     * Downloads a file from the specified URL.
     */
    suspend fun download(url: String): ByteArray = client.get(url).body()
    
    /**
     * Closes the HTTP client.
     */
    fun close() {
        client.close()
    }
}

/**
 * Creates a new Door43HttpClient instance.
 */
fun createHttpClient(): Door43HttpClient = Door43HttpClient()