package org.bibletranslationtools.writer

import io.github.vinceglb.filekit.PlatformFile
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.ImportProjects
import java.io.File
import java.io.InputStream
import java.lang.reflect.Field
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
        } catch (e: NoSuchFieldException) {
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
}
