package org.bibletranslationtools.writer.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.TargetTranslationMigrator.Companion.LATEST_VERSION
import org.bibletranslationtools.writer.core.manifest.Manifest
import org.bibletranslationtools.writer.core.manifest.toType
import org.bibletranslationtools.writer.rendering.USXtoUSFMConverter
import java.io.File

class TargetTranslationMigrator(
    private val directoryProvider: DirectoryProvider,
    private val catalogClient: ResourceCatalogClient
) {
    companion object {
        const val TAG = "TargetTranslationMigrator"
        private const val EARLIEST_VERSION = 2
        private const val LATEST_VERSION = 8

        private val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    suspend fun migrateManifest(manifest: String): String? {
        val tempDir = directoryProvider.createTempDir(System.currentTimeMillis().toString())
        val fakeTranslationDir = File(tempDir, "translation")
        fakeTranslationDir.mkdirs()
        return try {
            val manifestFile = File(fakeTranslationDir, Manifest.MANIFEST_FILE)
            manifestFile.writeText(manifest)
            migrate(fakeTranslationDir, manifestFile)
            manifestFile.readText()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to migrate manifest string", e)
            null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun migrate(
        targetTranslationDir: File,
        manifestFile: File = File(targetTranslationDir, Manifest.MANIFEST_FILE),
    ): File? {
        return try {
            val raw = json.parseToJsonElement(manifestFile.readText()).jsonObject
            val packageVersion = raw["package_version"]?.jsonPrimitive?.intOrNull
                ?: EARLIEST_VERSION

            val migratedDir = runMigrations(targetTranslationDir, packageVersion)

            if (!validateTranslationType(targetTranslationDir)) null else migratedDir
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
            { v2(it) },
            { v3(it) },
            { v4(it) },
            { v5(it) },
            { v6(it) },
            { v7(it) },
            { v8(it) },
        )
        val startIndex = (fromVersion - EARLIEST_VERSION).coerceIn(0, migrations.size)
        return migrations.drop(startIndex).fold(dir) { acc, migrate -> migrate(acc) }
    }

    // -----------------------------------------------------------------------
    // Migration steps
    // -----------------------------------------------------------------------

    private fun v2(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v2 = json.decodeFromString<ManifestV2>(manifestFile.readText())

        val finishedFrames = (v2.frames.filter { it.value.finished }.keys + v2.finishedFrames)
            .toMutableList()
        val finishedTitles = (v2.chapters.filter { it.value.finishedTitle }.keys + v2.finishedTitles)
            .toMutableList()
        val finishedReferences = (v2.chapters.filter { it.value.finishedReference }.keys + v2.finishedReferences)
            .toMutableList()

        val projectId = v2.projectId ?: v2.slug ?: ""
        val targetLanguageId = v2.targetLanguage.id ?: v2.targetLanguage.slug ?: ""
        val targetLanguage = TargetLanguage(
            slug = targetLanguageId,
            name = v2.targetLanguage.name,
            direction = v2.targetLanguage.direction,
        )

        // Normalize the legacy mixed-shape translators list (strings + {name: ...} objects)
        // into a clean List<String> so ManifestV3 can stay typed.
        val translators = v2.translators.mapNotNull { it.toTranslatorName() }

        val v3 = ManifestV3(
            packageVersion = 3,
            projectId = projectId,
            targetLanguage = targetLanguage,
            translators = translators,
            finishedFrames = finishedFrames,
            finishedTitles = finishedTitles,
            finishedReferences = finishedReferences,
        )
        manifestFile.writeText(json.encodeToString(v3))
        return path
    }

    private fun v3(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)

        // If a manifest enters the chain already at package_version 3 (skipping v2),
        // its translators may still be in legacy mixed shape. Sanitize at the JSON
        // tree level so the typed decode below succeeds.
        val sanitized = sanitizeTranslators(manifestFile.readText())
        manifestFile.writeText(sanitized)

        val v3 = json.decodeFromString<ManifestV3>(sanitized)
        val updated = v3.copy(packageVersion = 4)
        manifestFile.writeText(json.encodeToString(updated))

        migrateChunkChanges(path, v3.projectId)
        return path
    }

    private fun v4(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v3 = json.decodeFromString<ManifestV3>(manifestFile.readText())

        val typeSlug = v3.type?.slug ?: "text"
        val type = ResourceType.get(typeSlug)?.toType() ?: Manifest.Type(typeSlug, "")

        val project = v3.project
            ?: v3.projectId.takeIf { it.isNotEmpty() }
                ?.let { Manifest.Project(it, it.uppercase()) }
            ?: Manifest.Project("", "")

        val resource: Manifest.Resource? = if (type.slug == "text") {
            v3.resource
                ?: v3.resourceId?.let(::resourceFromId)
                ?: defaultResourceFor(project.slug)
        } else null

        val sourceTranslations = parseSourceTranslations(v3.sourceTranslations)

        val parentDraft = v3.parentDraftResourceId
            ?.let { Manifest.Draft(resourceSlug = it, comments = "The parent draft is unknown") }
            ?: Manifest.Draft()

        val finishedChunks = buildList {
            addAll(v3.finishedFrames)
            v3.finishedTitles.forEach { add("$it-title") }
            v3.finishedReferences.forEach { add("$it-reference") }
            v3.finishedProjectComponents.forEach { add("00-$it") }
        }

        val format = v3.format
            ?.takeIf { it.isNotEmpty() && it != "usx" && it != "default" }
            ?: if (type.slug != "text" || project.slug == "obs") "markdown" else "usfm"

        // Move project title file: title.txt → 00/title.txt
        val oldProjectTitle = File(path, "title.txt")
        if (oldProjectTitle.exists()) {
            val newProjectTitle = File(path, "00/title.txt")
            newProjectTitle.parentFile?.mkdirs()
            oldProjectTitle.renameTo(newProjectTitle)
        }

        val v4 = ManifestV4(
            packageVersion = 5,
            project = project,
            type = type,
            resource = resource,
            targetLanguage = v3.targetLanguage,
            translators = v3.translators,
            finishedChunks = finishedChunks,
            sourceTranslations = sourceTranslations,
            parentDraft = parentDraft,
            format = format,
        )
        manifestFile.writeText(json.encodeToString(v4))

        if (format == "usfm") {
            convertUsxToUsfmInPlace(path)
        }
        return path
    }

    private suspend fun v5(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v5 = json.decodeFromString<ManifestV5>(manifestFile.readText())

        val targetLanguageCode = v5.targetLanguage.slug
        val projectSlug = v5.project.slug
        val translationTypeSlug = v5.type.slug
        val resourceSlug = if (translationTypeSlug == "text") v5.resource?.slug else null

        val id = buildString {
            append("${targetLanguageCode}_${projectSlug}_$translationTypeSlug")
            if (translationTypeSlug == "text" && resourceSlug != null) append("_$resourceSlug")
        }

        ensureLicenseFile(path)

        val updated = v5.copy(packageVersion = 6)
        manifestFile.writeText(json.encodeToString(updated))

        val newPath = File(path.parentFile, id.lowercase())
        newPath.deleteRecursively()
        path.renameTo(newPath)
        return newPath
    }

    private fun v6(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v6 = json.decodeFromString<ManifestV6>(manifestFile.readText())
        val projectSlug = v6.project.slug

        val chapterDirs = path.listFiles { file ->
            file.isDirectory && file.name != ".git" && file.name != "cache"
        }.orEmpty()

        var updatedFinishedChunks = v6.finishedChunks.toMutableList()

        val translations = catalogClient.library.findTranslations(
            "en", projectSlug, null, "book", null, 3, -1
        )
        if (translations.isNotEmpty()) {
            val sourceTranslation = translations.find { it.resource.slug == "ulb" }
                ?: translations.first()
            val container = catalogClient.openResourceContainer(sourceTranslation.resourceContainerSlug)

            for (dir in chapterDirs) {
                val chunk00 = File(dir, "00.txt")
                if (!chunk00.exists()) continue

                val chunkId = largestIntVal(container.chunks(dir.name).toList()) ?: continue
                val newChunk = File(dir, "$chunkId.txt")
                if (chunk00.renameTo(newChunk)) {
                    val old = "${dir.name}-00"
                    val new = "${dir.name}-$chunkId"
                    updatedFinishedChunks = updatedFinishedChunks
                        .map { if (it == old) new else it }
                        .toMutableList()
                }
            }
        }

        // 00 chapter → front
        val chapter00 = File(path, "00")
        if (chapter00.exists() && chapter00.isDirectory) {
            chapter00.renameTo(File(path, "front"))
        }

        val updated = v6.copy(
            packageVersion = 7,
            finishedChunks = updatedFinishedChunks,
        )
        manifestFile.writeText(json.encodeToString(updated))
        return path
    }

    private fun v7(path: File): File {
        val manifestFile = File(path, Manifest.MANIFEST_FILE)
        val v7 = json.decodeFromString<ManifestV7>(manifestFile.readText())

        val resourceName = v7.resource.name.ifEmpty { defaultResourceName(v7.resource.slug) }

        val updated = v7.copy(
            packageVersion = 8,
            resource = v7.resource.copy(name = resourceName),
        )
        manifestFile.writeText(json.encodeToString(updated))
        return path
    }

    private fun v8(path: File): File = path

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Normalizes the `translators` field of a raw manifest JSON into a list of strings.
     * Used when entering the migration chain at v3 with legacy mixed-shape data.
     */
    private fun sanitizeTranslators(rawJson: String): String {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val translators = root["translators"] as? JsonArray ?: return rawJson

        val cleaned = translators.mapNotNull { it.toTranslatorName() }
        val updated = JsonObject(
            root.toMutableMap().apply {
                put("translators", JsonArray(cleaned.map { JsonPrimitive(it) }))
            }
        )
        return json.encodeToString(updated)
    }

    /**
     * Extracts a translator name from either a JSON string or a `{"name": "..."}` object.
     */
    private fun JsonElement.toTranslatorName(): String? = when {
        this is JsonPrimitive && isString -> content
        this is JsonObject -> this["name"]?.jsonPrimitive?.content
        else -> null
    }

    private fun parseSourceTranslations(element: JsonElement?): List<Manifest.Source> {
        return when (element) {
            is JsonArray -> json.decodeFromJsonElement<List<Manifest.Source>>(element)
            is JsonObject -> element.entries.mapNotNull { (key, value) ->
                runCatching {
                    val parts = key.split("-", limit = 2)
                    if (parts.size != 2) return@runCatching null

                    val languageResourceId = parts[1]
                    val pieces = languageResourceId.split("-")
                    if (pieces.isEmpty()) return@runCatching null

                    val resId = pieces.last()
                    val langId = languageResourceId.dropLast(resId.length + 1)
                    val obj = value.jsonObject

                    Manifest.Source(
                        languageSlug = langId,
                        resourceSlug = resId,
                        checkingLevel = obj["checking_level"]!!.jsonPrimitive.content,
                        modifiedAt = obj["date_modified"]!!.jsonPrimitive.content,
                        version = obj["version"]!!.jsonPrimitive.content,
                    )
                }.getOrNull()
            }
            else -> emptyList()
        }
    }

    private fun resourceFromId(id: String): Manifest.Resource = when (id) {
        "ulb" -> Manifest.Resource("ulb", "Unlocked Literal Bible")
        "udb" -> Manifest.Resource("udb", "Unlocked Dynamic Bible")
        "obs" -> Manifest.Resource("obs", "Open Bible Stories")
        else -> Manifest.Resource("reg", "Regular")
    }

    private fun defaultResourceFor(projectSlug: String): Manifest.Resource =
        if (projectSlug == "obs") {
            Manifest.Resource("obs", "Open Bible Stories")
        } else {
            Manifest.Resource("reg", "Regular")
        }

    private fun defaultResourceName(slug: String): String = when (slug) {
        "reg" -> "Regular"
        "obs" -> "Open Bible Stories"
        "udb" -> "Unlocked Dynamic Bible"
        "ulb" -> "Unlocked Literal Bible"
        else -> slug
    }

    private suspend fun ensureLicenseFile(path: File) {
        val licenseFile = File(path, TargetTranslation.LICENSE_FILE)
        if (licenseFile.exists()) return
        directoryProvider.getAssetAsFile("files/${TargetTranslation.LICENSE_FILE}").inputStream()
            .use { input -> licenseFile.outputStream().use { input.copyTo(it) } }
    }

    private fun convertUsxToUsfmInPlace(path: File) {
        val chapterDirs = path.listFiles { f -> f.isDirectory && f.name != ".git" }.orEmpty()
        for (chapterDir in chapterDirs) {
            chapterDir.listFiles().orEmpty().forEach { chunkFile ->
                runCatching {
                    val usfm = USXtoUSFMConverter.doConversion(chunkFile.readText()).toString()
                    chunkFile.writeText(usfm)
                }
            }
        }
    }

    private fun largestIntVal(list: List<String>): String? =
        list.mapNotNull { it.toIntOrNull() }.maxOrNull()?.toString()

    private fun migrateChunkChanges(targetTranslationDir: File, projectSlug: String) {
        if (projectSlug.isEmpty()) return

        val project = catalogClient.library.getProject("en", projectSlug, true) ?: return
        val resource = catalogClient.library.getResources(project.languageSlug, project.slug)
            .firstOrNull { it.type.equals("book", ignoreCase = true) }
            ?: return

        val resourceContainer = runCatching {
            catalogClient.openResourceContainer(project.languageSlug, project.slug, resource.slug)
        }.getOrElse { return }

        val chapterDirs = targetTranslationDir.listFiles { f ->
            f.isDirectory && f.name != ".git" && f.name != "00"
        } ?: return

        val manifestFile = File(targetTranslationDir, Manifest.MANIFEST_FILE)
        chapterDirs.forEach { mergeInvalidChunksInChapter(manifestFile, resourceContainer, it) }
    }

    private fun mergeInvalidChunksInChapter(
        manifestFile: File,
        resourceContainer: ResourceContainer,
        chapterDir: File,
    ): Boolean {
        val manifestV3 = runCatching {
            json.decodeFromString<ManifestV3>(manifestFile.readText())
        }.getOrElse { return false }

        val chunkMergeMarker = "\n----------\n"
        val chapterId = chapterDir.name
        val updatedFinishedFrames = manifestV3.finishedFrames.toMutableList()

        var frameFiles = chapterDir.listFiles { f ->
            f.name != "title.txt" && f.name != "reference.txt"
        }?.sortedArray() ?: return true

        var invalidChunks = ""
        var lastValidFrameFile: File? = null

        for (frameFile in frameFiles) {
            val frameId = frameFile.nameWithoutExtension
            val chunkText = resourceContainer.readChunk(chapterId, frameId)
            val frameBody = runCatching { frameFile.readText().trim() }.getOrDefault("")

            when {
                chunkText.isNotEmpty() -> {
                    lastValidFrameFile = frameFile
                    if (invalidChunks.isNotEmpty()) {
                        frameFile.writeText(invalidChunks + frameBody)
                        invalidChunks = ""
                        updatedFinishedFrames.remove("$chapterId-$frameId")
                    }
                }
                frameBody.isNotEmpty() -> {
                    if (lastValidFrameFile == null) {
                        invalidChunks += frameBody + chunkMergeMarker
                    } else {
                        val lastBody = runCatching { lastValidFrameFile.readText() }.getOrDefault("")
                        lastValidFrameFile.writeText(lastBody + chunkMergeMarker + frameBody)
                        updatedFinishedFrames.remove("$chapterId-${lastValidFrameFile.name}")
                    }
                    frameFile.delete()
                }
            }
        }

        if (invalidChunks.isNotEmpty()) {
            frameFiles = chapterDir.listFiles { f ->
                f.name != "title.txt" && f.name != "reference.txt"
            }?.sortedArray() ?: return true

            if (frameFiles.isNotEmpty()) {
                val firstBody = runCatching { frameFiles[0].readText() }.getOrDefault("")
                frameFiles[0].writeText(invalidChunks + chunkMergeMarker + firstBody)
                updatedFinishedFrames.remove("$chapterId-${frameFiles[0].name}")
            }
        }

        val updated = manifestV3.copy(finishedFrames = updatedFinishedFrames)
        manifestFile.writeText(json.encodeToString(updated))
        return true
    }

    private fun validateTranslationType(path: File): Boolean {
        val manifest = json.decodeFromString<ManifestV7>(
            File(path, Manifest.MANIFEST_FILE).readText()
        )
        return if (ResourceType.get(manifest.type.slug) == ResourceType.TEXT) {
            true
        } else {
            Logger.w(TAG, "Only text translation types are supported")
            false
        }
    }
}

// ---------------------------------------------------------------------------
// Version-specific manifest shapes
// ---------------------------------------------------------------------------

@Serializable
data class ManifestV2(
    @SerialName("package_version") val packageVersion: Int = 2,
    val slug: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    val frames: Map<String, FrameState> = emptyMap(),
    val chapters: Map<String, ChapterState> = emptyMap(),
    @SerialName("target_language") val targetLanguage: TargetLanguageV2,
    val translators: List<JsonElement> = emptyList(),
    @SerialName("finished_frames") val finishedFrames: List<String> = emptyList(),
    @SerialName("finished_titles") val finishedTitles: List<String> = emptyList(),
    @SerialName("finished_references") val finishedReferences: List<String> = emptyList(),
) {
    @Serializable
    data class FrameState(val finished: Boolean = false)

    @Serializable
    data class ChapterState(
        @SerialName("finished_title") val finishedTitle: Boolean = false,
        @SerialName("finished_reference") val finishedReference: Boolean = false,
    )

    @Serializable
    data class TargetLanguageV2(
        val id: String? = null,
        val slug: String? = null,
        val name: String,
        val direction: String,
    )
}

@Serializable
data class ManifestV3(
    @SerialName("package_version") val packageVersion: Int = 3,
    @SerialName("project_id") val projectId: String,
    @SerialName("target_language") val targetLanguage: TargetLanguage,
    val translators: List<String> = emptyList(),
    @SerialName("finished_frames") val finishedFrames: List<String> = emptyList(),
    @SerialName("finished_titles") val finishedTitles: List<String> = emptyList(),
    @SerialName("finished_references") val finishedReferences: List<String> = emptyList(),
    @SerialName("finished_project_components") val finishedProjectComponents: List<String> = emptyList(),
    @SerialName("source_translations") val sourceTranslations: JsonElement? = null,
    @SerialName("parent_draft_resource_id") val parentDraftResourceId: String? = null,
    val format: String? = null,
    val resource: Manifest.Resource? = null,
    @SerialName("resource_id") val resourceId: String? = null,
    val project: Manifest.Project? = null,
    val type: Manifest.Type? = null,
)

@Serializable
data class ManifestV4(
    @SerialName("package_version") val packageVersion: Int = 4,
    @SerialName("project_id") val projectId: String? = null,
    val project: Manifest.Project,
    val type: Manifest.Type,
    val resource: Manifest.Resource? = null,
    @SerialName("target_language") val targetLanguage: TargetLanguage,
    val translators: List<String> = emptyList(),
    @SerialName("finished_chunks") val finishedChunks: List<String> = emptyList(),
    @SerialName("source_translations") val sourceTranslations: List<Manifest.Source> = emptyList(),
    @SerialName("parent_draft") val parentDraft: Manifest.Draft = Manifest.Draft(),
    val format: String = "",
)

@Serializable
data class ManifestV5(
    @SerialName("package_version") val packageVersion: Int = 5,
    val project: Manifest.Project,
    val type: Manifest.Type,
    val resource: Manifest.Resource? = null,
    @SerialName("target_language") val targetLanguage: TargetLanguage,
    val translators: List<String> = emptyList(),
    @SerialName("finished_chunks") val finishedChunks: List<String> = emptyList(),
    @SerialName("source_translations") val sourceTranslations: List<Manifest.Source> = emptyList(),
    @SerialName("parent_draft") val parentDraft: Manifest.Draft = Manifest.Draft(),
    val format: String = "",
    val generator: Manifest.Generator = Manifest.Generator("", ""),
)

@Serializable
data class ManifestV6(
    @SerialName("package_version") val packageVersion: Int = 6,
    val project: Manifest.Project,
    val type: Manifest.Type,
    val resource: Manifest.Resource? = null,
    @SerialName("target_language") val targetLanguage: TargetLanguage,
    val translators: List<String> = emptyList(),
    @SerialName("finished_chunks") val finishedChunks: List<String> = emptyList(),
    @SerialName("source_translations") val sourceTranslations: List<Manifest.Source> = emptyList(),
    @SerialName("parent_draft") val parentDraft: Manifest.Draft = Manifest.Draft(),
    val format: String = "",
    val generator: Manifest.Generator = Manifest.Generator("", ""),
)

@Serializable
data class ManifestV7(
    @SerialName("package_version") val packageVersion: Int = 7,
    val project: Manifest.Project,
    val type: Manifest.Type,
    val resource: Manifest.Resource,
    @SerialName("target_language") val targetLanguage: TargetLanguage,
    val translators: List<String> = emptyList(),
    @SerialName("finished_chunks") val finishedChunks: List<String> = emptyList(),
    @SerialName("source_translations") val sourceTranslations: List<Manifest.Source> = emptyList(),
    @SerialName("parent_draft") val parentDraft: Manifest.Draft = Manifest.Draft(),
    val format: String = "",
    val generator: Manifest.Generator = Manifest.Generator("", ""),
)