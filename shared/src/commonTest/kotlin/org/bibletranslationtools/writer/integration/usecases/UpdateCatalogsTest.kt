package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Catalog
import org.bibletranslationtools.resourcecatalog.library.models.CatalogType
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.usecases.UpdateCatalogs
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Locale


class UpdateCatalogsTest : BaseIntegrationTest() {

    private val updateCatalogs: UpdateCatalogs by inject()
    private val catalogClient: ResourceCatalogClient by inject()

    override val needsLibrary = true

    @Before
    fun setUp() {
        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val successResponse = MockResponse.Builder().code(200)
                val notFoundResponse = MockResponse.Builder().code(404)

                return when (request.target) {
                    "/langnames.json" -> successResponse.body(runBlocking {
                        createResponse("langnames")
                    })
                    "/temp-langs.json" -> successResponse.body(runBlocking {
                        createResponse("temp_langs")
                    })
                    "/approved-langs.json" -> successResponse.body(runBlocking {
                        createResponse("approved_temp_langs")
                    })
                    else -> notFoundResponse
                }.build()
            }
        }
        server.dispatcher = dispatcher
    }

    fun tearDown() {
        runBlocking { directoryProvider.deleteLibrary() }
    }

    @Test
    fun testUpdateCatalogs() {
        val result = runBlocking {
            prepareCatalogs()
            updateCatalogs.execute(false)
        }

        assertTrue("Update catalogs should succeed", result.success)
        assertEquals("Added 2 languages", 2, result.addedCount)

        verifyTargetLanguages()
    }

    private fun verifyTargetLanguages() {
        val targetLanguages = catalogClient.library.getTargetLanguages()
        assertEquals("There should be 4 target languages", 4, targetLanguages.size)

        val aaLang = targetLanguages.singleOrNull { it.slug == "aa" }
        assertNotNull("Afar language should not be null", aaLang)
        assertEquals("Afar language slug should match", "aa", aaLang?.slug)
        assertEquals("Afar language name should match", "Qafar af New", aaLang?.name)
        assertEquals("Afar language anglicized name should match", "Afar New", aaLang?.anglicizedName)

        val test2Lang = targetLanguages.singleOrNull { it.slug == "test2" }
        assertNotNull("Test 2 language should not be null", test2Lang)
        assertEquals("Test 2 language slug should match", "test2", test2Lang?.slug)
        assertEquals("Test 2 language name should match", "Test 2", test2Lang?.name)
        assertEquals("Test 2 language anglicized name should match", "Test 2 Ang", test2Lang?.anglicizedName)

        val temp1Language = targetLanguages.singleOrNull { it.slug == "qaa-x-111111" }
        assertNotNull("Temp language 1 should not be null", temp1Language)
        assertEquals("Temp language slug should match", "qaa-x-111111", temp1Language?.slug)
        assertEquals("Temp language name should match", "Test Temp 1", temp1Language?.name)

        val temp2Language = targetLanguages.singleOrNull { it.slug == "qaa-x-222222" }
        assertNull("Temp language 2 should be null", temp2Language)

        val temp2LanguageApproved = catalogClient.library.getApprovedTargetLanguage("qaa-x-222222")
        assertEquals("Temp language slug should match", "ifk-x-yattuca", temp2LanguageApproved?.slug)
        assertEquals("Temp language name should match", "Yattuca", temp2LanguageApproved?.name)
    }

    private suspend fun prepareCatalogs() {
        val langCatalogUrl = server.url("/langnames.json").toString()
        val langCatalog = Catalog(CatalogType.TARGET_LANGUAGES, langCatalogUrl, 0)
        catalogClient.library.addCatalog(langCatalog)
        createResponse("langnames")

        val tempLangsCatalogUrl = server.url("/temp-langs.json").toString()
        val tempLangsCatalog = Catalog(CatalogType.TEMP_LANGUAGES, tempLangsCatalogUrl, 0)
        catalogClient.library.addCatalog(tempLangsCatalog)
        createResponse("temp_langs")

        val approvedLangsCatalogUrl = server.url("/approved-langs.json").toString()
        val approvedLangsCatalog = Catalog(CatalogType.APPROVED_LANGUAGES, approvedLangsCatalogUrl, 0)
        catalogClient.library.addCatalog(approvedLangsCatalog)
        createResponse("approved_temp_langs")
    }

    private suspend fun createResponse(id: String): String {
        val baseUrl = server.url("/").toString()
        val datetimeFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX", Locale.US)
        val project = directoryProvider.createTempFile(id, ".json")
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
