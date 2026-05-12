package org.bibletranslationtools.writer.unit.core

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ArchiveMigratorTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider

    private lateinit var migrator: ArchiveMigrator
    private lateinit var rootTempDir: File

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        migrator = ArchiveMigrator(directoryProvider)

        rootTempDir = createTempDirectory(prefix = "archive-migrator-test").toFile()
        coEvery { directoryProvider.createTempDir(any()) } answers {
            File(rootTempDir, java.util.UUID.randomUUID().toString()).apply { mkdirs() }
        }
        coEvery { directoryProvider.createTempDir() } answers {
            File(rootTempDir, java.util.UUID.randomUUID().toString()).apply { mkdirs() }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        rootTempDir.deleteRecursively()
    }

    @Test
    fun `migrateManifest from v1 transforms projects and commit_hash`() = runTest {
        val v1 = """
            {
              "package_version": 1,
              "timestamp": 1234567890,
              "generator": {"name": "ts-android", "build": "123"},
              "projects": [
                {
                  "id": "aa_mrk_text_reg",
                  "path": "aa_mrk_text_reg",
                  "direction": "ltr",
                  "commit_hash": {
                    "stdout": "abc123",
                    "stderr": "",
                    "error": null
                  }
                }
              ]
            }
        """.trimIndent()

        val migrated = migrator.migrateManifest(v1)

        assertNotNull(migrated)
        val obj = Json.parseToJsonElement(migrated!!).jsonObject
        assertEquals(3, obj["package_version"]!!.jsonPrimitive.content.toInt())
        val translations = obj["target_translations"]!!.jsonArray
        assertEquals(1, translations.size)
        val first = translations[0].jsonObject
        assertEquals("aa_mrk_text_reg", first["id"]!!.jsonPrimitive.content)
        assertEquals("abc123", first["commit_hash"]!!.jsonPrimitive.content)
    }

    @Test
    fun `migrateManifest from v2 bumps package_version to latest`() = runTest {
        val v2 = """
            {
              "package_version": 2,
              "timestamp": 0,
              "generator": {"name": "ts-android", "build": "1"},
              "target_translations": [
                {"id": "aa_mrk_text_reg", "path": "p", "direction": "ltr", "commit_hash": "deadbeef"}
              ]
            }
        """.trimIndent()

        val migrated = migrator.migrateManifest(v2)

        assertNotNull(migrated)
        val obj = Json.parseToJsonElement(migrated!!).jsonObject
        assertEquals(3, obj["package_version"]!!.jsonPrimitive.content.toInt())
        val translations = obj["target_translations"]!!.jsonArray
        assertEquals("deadbeef", translations[0].jsonObject["commit_hash"]!!.jsonPrimitive.content)
    }

    @Test
    fun `migrateManifest from v3 leaves package_version unchanged`() = runTest {
        val v3 = """
            {
              "package_version": 3,
              "timestamp": 0,
              "generator": {"name": "ts-android", "build": "1"},
              "target_translations": [
                {"id": "aa_mrk_text_reg", "path": "p", "direction": "ltr", "commit_hash": "h"}
              ]
            }
        """.trimIndent()

        val migrated = migrator.migrateManifest(v3)

        assertNotNull(migrated)
        val obj = Json.parseToJsonElement(migrated!!).jsonObject
        assertEquals(3, obj["package_version"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `migrateManifest with invalid json returns input unchanged`() = runTest {
        // migrate() fails internally; migrateManifest then echoes back the written file.
        val migrated = migrator.migrateManifest("not-json")
        assertEquals("not-json", migrated)
    }

    @Test
    fun `migrate returns null on unparseable manifest`() = runTest {
        val fakeArchive = File(rootTempDir, "garbage").apply { mkdirs() }
        File(fakeArchive, "manifest.json").writeText("not-json")
        assertNull(migrator.migrate(fakeArchive))
    }

    @Test
    fun `migrateManifest cleans up its temp directory`() = runTest {
        val v3 = """
            {
              "package_version": 3,
              "timestamp": 0,
              "generator": {"name": "ts-android", "build": "1"},
              "target_translations": []
            }
        """.trimIndent()

        migrator.migrateManifest(v3)

        val leftovers = rootTempDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        assertEquals("Temp dir should be cleaned up", 0, leftovers.size)
    }

    @Test
    fun `migrate on disk writes migrated manifest back to file`() = runTest {
        val fakeArchive = File(rootTempDir, "archive").apply { mkdirs() }
        val manifestFile = File(fakeArchive, "manifest.json")
        manifestFile.writeText(
            """
            {
              "package_version": 1,
              "timestamp": 0,
              "generator": {"name": "ts-android", "build": "1"},
              "projects": [
                {
                  "id": "aa_mrk_text_reg",
                  "path": "p",
                  "direction": "ltr",
                  "commit_hash": {"stdout": "h", "stderr": "", "error": null}
                }
              ]
            }
            """.trimIndent()
        )

        val result = migrator.migrate(fakeArchive)

        assertNotNull(result)
        val obj = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        assertEquals(3, obj["package_version"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `migrate returns null when manifest cannot be parsed after migration`() = runTest {
        val fakeArchive = File(rootTempDir, "broken").apply { mkdirs() }
        val manifestFile = File(fakeArchive, "manifest.json")
        // missing required fields for final ArchiveManifest validation
        manifestFile.writeText("""{"package_version": 3}""")

        val result = migrator.migrate(fakeArchive)

        assertNull(result)
        // sanity — the manifest file is still there but the migrator rejected it
        assertFalse(manifestFile.readText().isEmpty())
    }
}
