package org.bibletranslationtools.writer.core

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecontainer.IntAsStringSerializer
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.ArchiveMigrator.Companion.LATEST_VERSION
import org.bibletranslationtools.writer.core.manifest.Manifest
import java.io.File

class ArchiveMigrator(
    private val directoryProvider: DirectoryProvider
) {
    companion object {
        private const val TAG = "ArchiveMigrator"
        private const val EARLIEST_VERSION = 1
        private const val LATEST_VERSION = 3

        const val MANIFEST_JSON: String = "manifest.json"

        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            encodeDefaults = true

            serializersModule = SerializersModule {
                contextual(String::class, IntAsStringSerializer)
            }
        }
    }

    suspend fun migrateManifest(manifest: String): String? {
        val fakeArchiveDir = directoryProvider.createTempDir()
        fakeArchiveDir.mkdirs()
        return try {
            val manifestFile = File(fakeArchiveDir, Manifest.MANIFEST_FILE)
            manifestFile.writeText(manifest)
            migrate(fakeArchiveDir, manifestFile)
            manifestFile.readText()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to migrate archive manifest string", e)
            null
        } finally {
            fakeArchiveDir.deleteRecursively()
        }
    }

    suspend fun migrate(
        fakeArchiveDir: File,
        manifestFile: File = File(fakeArchiveDir, Manifest.MANIFEST_FILE),
    ): File? {
        return try {
            val raw = json.parseToJsonElement(manifestFile.readText()).jsonObject
            val packageVersion = raw["package_version"]?.jsonPrimitive?.intOrNull
                ?: EARLIEST_VERSION

            val migratedDir = runMigrations(fakeArchiveDir, packageVersion)

            if (!validateTranslationType(fakeArchiveDir)) null else migratedDir
        } catch (e: Exception) {
            Logger.e(TAG, "Migration failed", e)
            null
        }
    }

    /**
     * Runs all migration steps from [fromVersion] up to [LATEST_VERSION] in order.
     * Adding a new migration is a one-line change to the migrations list.
     */
    private suspend fun runMigrations(dir: File, fromVersion: Int): File {
        val migrations: List<suspend (File) -> File> = listOf(
            { v1(it) },
            { v2(it) },
            { v3(it) }
        )
        val startIndex = (fromVersion - EARLIEST_VERSION).coerceIn(0, migrations.size)
        return migrations.drop(startIndex).fold(dir) { acc, migrate -> migrate(acc) }
    }

    // -----------------------------------------------------------------------
    // Migration steps
    // -----------------------------------------------------------------------

    private fun v1(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v1 = json.decodeFromString<ArchiveManifestV1>(manifestFile.readText())

        val v2 = ArchiveManifestV2(
            packageVersion = 2,
            timestamp = v1.timestamp,
            generator = v1.generator,
            targetTranslations = v1.targetTranslations.map {
                ArchiveTranslationV2(
                    id = it.id,
                    path = it.path,
                    direction = it.direction,
                    commitHash = it.commitHash?.stdout
                )
            }
        )
        manifestFile.writeText(json.encodeToString(v2))
        return path
    }

    private fun v2(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)

        val v2 = json.decodeFromString<ArchiveManifestV2>(manifestFile.readText())
        val updated = v2.copy(packageVersion = 3)
        manifestFile.writeText(json.encodeToString(updated))
        return path
    }

    private fun v3(path: File): File = path

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun validateTranslationType(path: File): Boolean {
        return try {
            json.decodeFromString<ArchiveManifest>(
                File(path, Manifest.MANIFEST_FILE).readText()
            )
            true
        } catch (e: Exception) {
            Logger.w(TAG, "Manifest was not migrated correctly", e)
            false
        }
    }
}

object CommitHashSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                val stdout = element["stdout"]?.jsonPrimitive?.content ?: ""
                JsonPrimitive(stdout)
            }
            else -> element
        }
    }
}

// ---------------------------------------------------------------------------
// Version-specific manifest shapes
// ---------------------------------------------------------------------------

@Serializable
data class ArchiveManifestV1(
    @SerialName("package_version")
    val packageVersion: Int,
    val timestamp: Long,
    val generator: ArchiveGenerator,
    @SerialName("projects")
    val targetTranslations: List<ArchiveTranslationV1> = emptyList()
)

@Serializable
data class ArchiveTranslationV1(
    val id: String,
    val path: String,
    val direction: String,
    @SerialName("commit_hash")
    val commitHash: CommitHash?
)

@Serializable
data class CommitHash(
    val stdout: String,
    val stderr: String,
    val error: String?
)

@Serializable
data class ArchiveManifestV2(
    @SerialName("package_version")
    val packageVersion: Int,
    val timestamp: Long,
    val generator: ArchiveGenerator,
    @SerialName("target_translations")
    val targetTranslations: List<ArchiveTranslationV2> = emptyList()
)

@Serializable
data class ArchiveTranslationV2(
    val id: String,
    val path: String,
    val direction: String,
    @SerialName("commit_hash")
    @Serializable(with = CommitHashSerializer::class)
    val commitHash: String?
)

@Serializable
data class ArchiveManifest(
    @SerialName("package_version")
    val packageVersion: Int,
    val timestamp: Long,
    val generator: ArchiveGenerator,
    @SerialName("target_translations")
    val targetTranslations: List<ArchiveTranslation> = emptyList()
)

@Serializable
data class ArchiveGenerator(
    val name: String,
    @Contextual
    val build: String
)

@Serializable
data class ArchiveTranslation(
    val id: String,
    val path: String,
    val direction: String,
    @SerialName("commit_hash")
    val commitHash: String?
)
