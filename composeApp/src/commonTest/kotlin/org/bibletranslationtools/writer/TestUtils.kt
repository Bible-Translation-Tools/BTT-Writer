package org.bibletranslationtools.writer

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_token_name
import io.github.vinceglb.filekit.PlatformFile
import junit.framework.TestCase
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.InputStream
import java.lang.reflect.Field
import java.util.UUID
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertNotNull

object TestUtils {

    /**
     * Sets a property of an object using reflection
     * @param obj the source object
     * @param fieldName the name of the field
     * @param value the value to set
     */
    fun setPropertyReflection(obj: Any, fieldName: String, value: Any) {
        val cls = obj::class.java
        val field = findField(cls, fieldName)
        field?.isAccessible = true
        field?.set(obj, value)
    }

    /**
     * Finds a field in a class hierarchy
     */
    private fun findField(cls: Class<*>, fieldName: String): Field? {
        var field: Field? = null
        try {
            field = cls.getDeclaredField(fieldName)
        } catch (_: NoSuchFieldException) {
            if (cls.superclass != null) {
                field = findField(cls.superclass, fieldName)
            }
        }
        return field
    }

    /**
     * Gets a resource from the test resources folder as text
     */
    fun getResource(name: String): String {
        return this::class.java.classLoader
            ?.getResourceAsStream(name)
            ?.bufferedReader()
            ?.readText()
            ?: error("Resource not found: $name")
    }

    /**
     * Opens a resource from the test resources folder as a stream
     */
    fun getResourceStream(name: String): InputStream {
        return this::class.java.classLoader
            ?.getResourceAsStream(name)
            ?: error("Resource not found: $name")
    }

    /**
     * import USFM file to be used for testing
     * @param catalogClient
     * @param platform
     * @param directoryProvider
     * @param profile
     * @param importProjects
     * @param translator
     * @param langCode
     * @param path resource path (e.g. "usfm/mrk.usfm")
     * @return created TargetTranslation
     */
    suspend fun importTargetTranslation(
        catalogClient: ResourceCatalogClient,
        platform: Platform,
        directoryProvider: DirectoryProvider,
        profile: Profile,
        importProjects: ImportProjects,
        translator: Translator,
        langCode: String,
        path: String
    ): TargetTranslation? {
        val targetLanguage = catalogClient.library.getTargetLanguage(langCode)
        val processUSFM = ProcessUSFM(platform, directoryProvider, profile, catalogClient)

        val text = getResource(path)
        val filename = path.substringAfterLast("/")
        val tempDir = File.createTempFile("usfm_test_dir_", "").apply { delete(); mkdirs(); deleteOnExit() }
        val tempFile = File(tempDir, filename).apply { writeText(text); deleteOnExit() }
        val platformFile = PlatformFile(tempFile)

        val session = processUSFM.startImport(targetLanguage!!, platformFile)

        assertTrue("import usfm test file should succeed", session.isSuccess)
        val imports = session.importedProjects
        assertEquals("import usfm test file should succeed", 1, imports.size)

        val projectFolder = imports[0]

        val result = importProjects.importProject(projectFolder, true)

        assertNotNull(result, "Import result should not be null")
        assertNotNull(result.importedSlug, "importedSlug should not be null")

        return translator.getTargetTranslation(result.importedSlug)
    }

    suspend fun importTargetTranslation(
        importProjects: ImportProjects,
        translator: Translator,
        directoryProvider: DirectoryProvider,
        path: String
    ): TargetTranslation? {
        val projectFile = directoryProvider.createTempFile("project", ".tstudio")
        val text = getResource(path)
        projectFile.writeText(text)

        assertTrue("Project file should exist", projectFile.exists())
        assertTrue("Project file should not be empty", projectFile.length() > 0)

        val result = importProjects.importProject(projectFile, true)
        return translator.getTargetTranslation(result!!.importedSlug!!)
    }

    suspend fun simulateLoginGogsUser(
        platform: Platform,
        server: MockWebServer,
        gogsLogin: GogsLogin,
        username: String,
        fullName: String? = null
    ): User {
        server.enqueue(createLoginResponse(username, fullName))
        server.enqueue(createGetTokenResponse(platform))
        server.enqueue(MockResponse().setResponseCode(204)) // Delete token response
        server.enqueue(createTokenResponse(platform))

        val result = gogsLogin.execute("username", "password", fullName)

        TestCase.assertNotNull("User should not be null", result.user)

        val user = result.user!!

        TestCase.assertEquals(username, user.username)
        TestCase.assertNotNull("Token should not be null", user.token)
        TestCase.assertTrue(
            "Token name should contain build model",
            user.token?.name?.contains(platform.udid) == true
        )

        return user
    }

    private fun createLoginResponse(username: String, fullName: String? = null): MockResponse {
        val body = """
            {"id": 1, "username": "$username", "full_name": "${fullName ?: ""}"}
        """.trimIndent()

        return MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200)
    }

    private suspend fun createGetTokenResponse(platform: Platform): MockResponse {
        val body = """
            [{"id": 1, "name": "${getTokenStub(platform)}", "sha1": "${generateHash()}"}]
        """.trimIndent()

        return MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200)
    }

    private suspend fun createTokenResponse(platform: Platform): MockResponse {
        val body = """
            {"id": 1, "name": "${getTokenStub(platform)}", "sha1": "${generateHash()}"}
        """.trimIndent()

        return MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(201)
    }

    suspend fun getTokenStub(platform: Platform): String {
        val defaultTokenName = getString(Res.string.gogs_token_name)
        val androidId = platform.info.device.lowercase()
        val nickname = platform.udid
        val tokenSuffix = String.format("%s_%s__%s", platform.info.manufacturer, nickname, androidId)
        return (defaultTokenName + "__" + tokenSuffix).replace(" ", "_")
    }

    fun generateHash(): String {
        return UUID.randomUUID().toString()
    }
}
