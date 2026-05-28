package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UpdateSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Locale


class UpdateSourceTest : BaseIntegrationTest() {

    private val catalogClient: ResourceCatalogClient by inject()
    private val updateSource: UpdateSource by inject()
    private val preference: Preference by inject()

    override val needsLibrary = true

    @Before
    fun setUp() {
        every { preference.getRootCatalogApi() } returns Preference.ROOT_CATALOG_API_URL

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val successResponse = MockResponse.Builder().code(200)
                val notFoundResponse = MockResponse.Builder().code(404)
                val isCatalog = request.target.endsWith("/catalog.json")

                return when {
                    request.target == "/mat" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("mat"))
                    request.target == "/mat_es" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("mat_es"))
                    request.target == "/mat_tpi" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("mat_tpi"))
                    request.target == "/mat_test" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("mat_test"))
                    request.target == "/luk" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("luk"))
                    request.target == "/luk_es" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("luk_es"))
                    request.target == "/luk_tpi" -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("luk_tpi"))
                    isCatalog -> successResponse
                        .addHeader("Content-Type", "application/json")
                        .body(createResponse("catalog"))
                    else -> notFoundResponse
                }.build()
            }
        }
        server.dispatcher = dispatcher
    }

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testUpdateSource() {
        val url = server.url("/test")
        every {
            preference.getPref(Preference.KEY_PREF_MEDIA_SERVER, any(), String::class)
        } returns url.toString()

        val result = runBlocking { updateSource.execute() }

        assertTrue("Update source succeeded", result.success)
        assertEquals("Added 1 source", 1, result.addedCount)
        assertEquals("Updated 6 sources", 6, result.updatedCount)

        val sourceLanguages = catalogClient.library.getSourceLanguages()

        assertNotNull(
            "Test Source language should be added",
            sourceLanguages.singleOrNull { it.slug == "test" && it.name == "Test Language" }
        )
        assertNotNull(
            "Spanish source language should exist",
            sourceLanguages.singleOrNull { it.slug == "es-419" && it.name == "Espa\u00f1ol (Latin American Spanish)" }
        )
        assertNotNull(
            "Tok Pisin source language should exist",
            sourceLanguages.singleOrNull { it.slug == "tpi" && it.name == "Tok Pisin" }
        )

        verifyTestProject()
        verifyLukProject()
    }

    private fun verifyTestProject() {
        val projects = catalogClient.library.getProjects("test", false)
        val project = projects.singleOrNull { it.slug == "mat" }

        assertEquals("There should be 2 test project", 2, projects.size)
        assertNotNull("Project should not be null", project)
        assertEquals("Project slug should match", "mat", project?.slug)
        assertEquals("Project name should match", "Matthew New", project?.name)
        assertEquals(
            "Project description should match",
            "Mateo Test",
            project?.description
        )
        assertTrue(
            "Project chunksUrl should match",
            project?.chunksUrl?.endsWith("mat/test/chunks.json") ?: false
        )
        assertEquals(
            "Project languageSlug should match",
            "test",
            project?.languageSlug
        )

        val twProject = projects.singleOrNull { it.slug == "bible" }
        assertNotNull("TW project should not be null", twProject)
        assertEquals("TW project slug should be bible", "bible", twProject?.slug)
        assertEquals("TW project name should match", "translationWords", twProject?.name)
        assertEquals("TW project languageSlug should match", "test", twProject?.languageSlug)

        val resources = catalogClient.library.getResources("test", "mat")
        assertTrue("Resources should not be empty", resources.isNotEmpty())

        val tstResource = resources.singleOrNull { it.slug == "tst" }
        assertNotNull("TST resource should not be null", tstResource)
        assertEquals(
            "TST resource name should match",
            "Test Unlocked Literal Bible",
            tstResource?.name
        )
        assertEquals("TST resource type should match", "book", tstResource?.type)
        assertEquals("TST resource version should match", "12.2", tstResource?.status?.version)
        assertTrue(
            "TST resource url should match",
            tstResource?.formats?.first()?.url?.endsWith("/mat/test/source.json") ?: false
        )

        val tnResource = resources.singleOrNull { it.slug == "tn" }
        assertNotNull("TN resource should not be null", tnResource)
        assertEquals("TN resource name should match", "translationNotes", tnResource?.name)
        assertEquals("TN resource type should match", "help", tnResource?.type)
        assertEquals("TN resource version should match", "12.2", tnResource?.status?.version)
        assertTrue(
            "TN resource url should match",
            tnResource?.formats?.first()?.url?.endsWith("/mat/test/notes.json") ?: false
        )

        val tqResource = resources.singleOrNull { it.slug == "tq" }
        assertNotNull("TQ resource should not be null", tqResource)
        assertEquals(
            "TQ resource name should match",
            "translationQuestions",
            tqResource?.name
        )
        assertEquals("TQ resource type should match", "help", tqResource?.type)
        assertEquals("TQ resource version should match", "12.2", tqResource?.status?.version)
        assertTrue("" +
                "TQ resource url should match",
            tqResource?.formats?.first()?.url?.endsWith("/mat/test/questions.json") ?: false
        )

        val twResource = catalogClient.library.getResource("test", "bible", "tw")
        assertNotNull("TW resource should not be null", twResource)
        assertEquals("TW resource name should match", "translationWords", twResource?.name)
        assertEquals("TW resource type should match", "dict", twResource?.type)
        assertEquals("TW resource version should match", "12.2", twResource?.status?.version)
        assertTrue(
            "TW resource url should be match",
            twResource?.formats?.first()?.url?.endsWith("/mat/test/words.json") ?: false
        )
    }

    private fun verifyLukProject() {
        val projects = catalogClient.library.getProjects("es-419", false)
        val project = projects.singleOrNull { it.slug == "luk" }

        assertEquals("There should be 67 test project", 67, projects.size)
        assertNotNull("Project should not be null", project)
        assertEquals("Project slug should match", "luk", project?.slug)
        assertEquals("Project name should match", "Lucas", project?.name)
        assertEquals(
            "Project description should match",
            "Lucas Spanish",
            project?.description
        )
        assertTrue(
            "Project chunksUrl should match",
            project?.chunksUrl?.endsWith("luk/es/chunks.json") ?: false
        )
        assertEquals(
            "Project languageSlug should match",
            "es-419",
            project?.languageSlug
        )
    }

    private fun createResponse(id: String): String {
        val baseUrl = server.url("/").toString()
        val datetimeFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX", Locale.US)
        val project = runBlocking { directoryProvider.createTempFile(id, ".json") }
        TestUtils.getResourceStream("catalog/$id.json").use { input ->
            project.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val datetime = datetimeFormat.format(System.currentTimeMillis())
        val date = dateFormat.format(System.currentTimeMillis())
        val text = project.readText()
            .replace("{server}/", baseUrl)
            .replace("{datetime}", datetime)
            .replace("{date}", date)

        return text
    }
}
