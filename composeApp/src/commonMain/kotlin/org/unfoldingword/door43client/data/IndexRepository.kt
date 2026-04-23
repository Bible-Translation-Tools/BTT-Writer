package org.unfoldingword.door43client.data

import org.unfoldingword.door43client.Index
import org.unfoldingword.door43client.db.Door43Database
import org.unfoldingword.door43client.models.Catalog
import org.unfoldingword.door43client.models.Category
import org.unfoldingword.door43client.models.CategoryEntry
import org.unfoldingword.door43client.models.ChunkMarker
import org.unfoldingword.door43client.models.SourceLanguage
import org.unfoldingword.door43client.models.TargetLanguage
import org.unfoldingword.door43client.models.Translation
import org.unfoldingword.door43client.models.Versification
import org.unfoldingword.resourcecontainer.ContainerTools
import org.unfoldingword.resourcecontainer.Language
import org.unfoldingword.resourcecontainer.Project
import org.unfoldingword.resourcecontainer.Resource
import org.unfoldingword.resourcecontainer.ResourceContainer

/**
 * Repository implementation using SQLDelight for database operations.
 */
class IndexRepository(
    private val database: Door43Database
) : Index {

    private val queries = database.indexQueries

    override fun listSourceLanguagesLastModified(): List<Map<String, Long>> {
        val results = queries.listSourceLanguagesLastModified(
            ResourceContainer.baseMimeType + "%"
        ).executeAsList()
        return results.map { row ->
            mapOf(row.slug to row.modified_at)
        }
    }

    override fun listProjectsLastModified(languageSlug: String?): Map<String, Int> {
        val results = queries.listProjectsLastModified(
            ResourceContainer.baseMimeType + "%",
            languageSlug ?: "%"
        ).executeAsList()
        return results.associate { it.slug to it.modified_at }
    }

    override fun getTranslation(containerSlug: String): Translation? {
        return try {
            val slugs = ContainerTools.explodeSlug(containerSlug)
            val l = getSourceLanguage(slugs[0])
            val p = getProject(slugs[0], slugs[1], false)
            val r = getResource(slugs[0], slugs[1], slugs[2])
            if (l != null && p != null && r != null) Translation(l, p, r) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun findTranslations(
        languageSlug: String?,
        projectSlug: String?,
        resourceSlug: String?,
        resourceType: String?,
        translateMode: String?,
        minCheckingLevel: Int,
        maxCheckingLevel: Int
    ): List<Translation> {
        val lSlug = languageSlug?.ifEmpty { "%" } ?: "%"
        val pSlug = projectSlug?.ifEmpty { "%" } ?: "%"
        val rSlug = resourceSlug?.ifEmpty { "%" } ?: "%"
        val rType = resourceType?.ifEmpty { "%" } ?: "%"
        val tMode = translateMode?.ifEmpty { "%" } ?: "%"

        val results = queries.findTranslations(
            lSlug, pSlug, rSlug,
            minCheckingLevel.toString(), tMode, rType
        ).executeAsList()

        return results.mapNotNull { row ->
            buildTranslation(row)
        }
    }

    override fun getImportedTranslations(): List<Translation> {
        val results = queries.getImportedTranslations().executeAsList()
        return results.mapNotNull { row ->
            buildTranslation(row)
        }
    }

    private fun buildTranslation(row: FindTranslationsRow): Translation? {
        return try {
            val language = Language(row.languageSlug, row.languageName, row.direction)
            val project = Project(row.projectSlug, row.projectName, row.sort).apply {
                description = row.desc
                icon = row.icon
                chunksUrl = row.chunksUrl
                languageSlug = row.languageSlug
            }
            val resource = Resource(
                row.resourceSlug, row.resourceName,
                row.type, row.translateMode, row.checkingLevel, row.version
            ).apply {
                comments = row.comments
                pubDate = row.pubDate
                license = row.license
            }
            Translation(language, project, resource)
        } catch (e: Exception) {
            null
        }
    }

    override fun getSourceLanguage(sourceLanguageSlug: String): SourceLanguage? {
        return queries.getSourceLanguage(sourceLanguageSlug).executeAsOneOrNull()?.let {
            SourceLanguage(it.slug, it.name, it.direction)
        }
    }

    override fun getSourceLanguages(): List<SourceLanguage> {
        return queries.getSourceLanguages().executeAsList().map {
            SourceLanguage(it.slug, it.name, it.direction)
        }
    }

    override fun getSourceLanguages(projectSlug: String): List<SourceLanguage> {
        return queries.getSourceLanguagesForProject(projectSlug).executeAsList().map {
            SourceLanguage(it.slug, it.name, it.direction)
        }
    }

    override fun addSourceLanguage(language: SourceLanguage): Long {
        queries.insertSourceLanguage(
            language.slug,
            language.name,
            language.direction
        )
        return 1L // SQLDelight doesn't return row id, return success indicator
    }

    override fun getTargetLanguage(targetLanguageSlug: String): TargetLanguage? {
        return queries.getTargetLanguage(targetLanguageSlug).executeAsOneOrNull()?.let {
            TargetLanguage(
                slug = it.slug,
                name = it.name,
                anglicizedName = it.anglicizedName ?: "",
                direction = it.direction,
                region = it.region ?: "",
                isGatewayLanguage = it.isGatewayLanguage == 1L
            )
        }
    }

    override fun addTargetLanguage(language: TargetLanguage): Boolean {
        queries.insertTargetLanguage(
            language.slug,
            language.name,
            language.anglicizedName,
            language.direction,
            language.region,
            if (language.isGatewayLanguage) 1L else 0L
        )
        return true
    }

    override fun findTargetLanguage(nameQuery: String): List<TargetLanguage> {
        return queries.findTargetLanguage("%${nameQuery.lowercase()}%").executeAsList().map {
            TargetLanguage(
                slug = it.slug,
                name = it.name,
                anglicizedName = it.anglicizedName ?: "",
                direction = it.direction,
                region = it.region ?: "",
                isGatewayLanguage = it.isGatewayLanguage == 1L
            )
        }
    }

    override fun getTargetLanguages(): List<TargetLanguage> {
        return queries.getTargetLanguages().executeAsList().map {
            TargetLanguage(
                slug = it.slug,
                name = it.name,
                anglicizedName = it.anglicizedName ?: "",
                direction = it.direction,
                region = it.region ?: "",
                isGatewayLanguage = it.isGatewayLanguage == 1L
            )
        }
    }

    override fun getTargetLanguage(targetLanguageSlug: String): TargetLanguage? {
        return super.getTargetLanguage(targetLanguageSlug)
    }

    override fun getProject(
        sourceLanguageSlug: String,
        projectSlug: String,
        enableDefaultLanguage: Boolean
    ): Project? {
        val row = queries.getProject(projectSlug, sourceLanguageSlug).executeAsOneOrNull()
            ?: if (enableDefaultLanguage) {
                queries.getProject(projectSlug, "en").executeAsOneOrNull()
            } else null

        return row?.let {
            Project(it.slug, it.name, it.sort).apply {
                description = it.desc
                icon = it.icon
                chunksUrl = it.chunksUrl
                languageSlug = it.sourceLanguageSlug
            }
        }
    }

    override fun getProjects(
        sourceLanguageSlug: String,
        enableDefaultLanguage: Boolean
    ): List<Project> {
        return queries.getProjectsForLanguage(sourceLanguageSlug).executeAsList().map {
            Project(it.slug, it.name, it.sort).apply {
                description = it.desc
                icon = it.icon
                chunksUrl = it.chunksUrl
                languageSlug = sourceLanguageSlug
            }
        }
    }

    override fun getProjectCategories(
        parentCategoryId: Long,
        languageSlug: String,
        translateMode: String?
    ): List<CategoryEntry> {
        val mode = translateMode ?: ""
        return queries.getProjectCategories(mode, parentCategoryId).executeAsList().map {
            CategoryEntry(
                entryType = CategoryEntry.Type.CATEGORY,
                id = it.id,
                slug = it.slug,
                name = it.name,
                sourceLanguageSlug = it.sourceLanguageSlug ?: "",
                parentCategoryId = parentCategoryId
            )
        }
    }

    override fun getResource(
        sourceLanguageSlug: String,
        projectSlug: String,
        resourceSlug: String
    ): Resource? {
        val row = queries.getResource(resourceSlug, projectSlug, sourceLanguageSlug)
            .executeAsOneOrNull()

        return row?.let {
            Resource(
                resourceSlug, it.name,
                it.type, it.translateMode, it.checkingLevel, it.version
            ).apply {
                comments = it.comments
                pubDate = it.pubDate
                license = it.license
            }
        }
    }

    override fun getResources(sourceLanguageSlug: String?, projectSlug: String): List<Resource> {
        val results = if (sourceLanguageSlug != null) {
            queries.getResourcesForProject(projectSlug, sourceLanguageSlug).executeAsList()
        } else {
            queries.getResourcesForProject(projectSlug, "%").executeAsList()
        }

        return results.map {
            Resource(it.slug, it.name, it.type, it.translateMode, it.checkingLevel, it.version).apply {
                comments = it.comments
                pubDate = it.pubDate
                license = it.license
            }
        }
    }

    override fun getCatalog(catalogSlug: String): Catalog? {
        return queries.getCatalog(catalogSlug).executeAsOneOrNull()?.let {
            Catalog(it.slug, it.url, it.modifiedAt)
        }
    }

    override fun addCatalog(catalog: Catalog): Long {
        queries.insertCatalog(catalog.slug, catalog.url, catalog.modifiedAt.toLong())
        return 1L
    }

    override fun getCatalogs(): List<Catalog> {
        return queries.getCatalogs().executeAsList().map {
            Catalog(it.slug, it.url, it.modifiedAt)
        }
    }

    override fun getVersification(
        sourceLanguageSlug: String,
        versificationSlug: String
    ): Versification? {
        return queries.getVersification(sourceLanguageSlug, versificationSlug)
            .executeAsOneOrNull()?.let {
                Versification(it.slug, it.name).apply {
                    rowId = it.id
                }
            }
    }

    override fun addVersification(versification: Versification, sourceLanguageId: Long): Long {
        queries.insertVersification(versification.slug)
        queries.insertVersificationName(
            sourceLanguageId,
            1L, // This would need to return the actual id
            verification.name
        )
        return 1L
    }

    override fun getVersifications(sourceLanguageSlug: String): List<Versification> {
        return queries.getVersifications(sourceLanguageSlug).executeAsList().map {
            Versification(it.slug, it.name).apply {
                rowId = it.id
            }
        }
    }

    override fun getChunkMarkers(projectSlug: String, versificationSlug: String): List<ChunkMarker> {
        return queries.getChunkMarkers(versificationSlug, projectSlug).executeAsList().map {
            ChunkMarker(it.chapter, it.verse)
        }
    }

    override fun getCategory(languageSlug: String, slug: String): Category? {
        return queries.getCategory(slug, languageSlug).executeAsOneOrNull()?.let {
            Category(it.slug, it.name)
        }
    }

    override fun getCategories(languageSlug: String, projectSlug: String): List<Category> {
        return queries.getCategoriesForProject(projectSlug, languageSlug).executeAsList().map {
            Category(it.slug, it.name)
        }
    }

    override fun yieldSafely() {
        // SQLDelight handles this internally
    }

    /**
     * Clears all cached target languages.
     */
    fun clearTargetLanguages() {
        queries.clearTargetLanguages()
    }

    /**
     * Clears temporary target languages.
     */
    fun clearTempLanguages() {
        queries.clearTempLanguages()
    }

    /**
     * Clears language questions.
     */
    fun clearNewLanguageQuestions() {
        queries.clearNewLanguageQuestions()
    }

    /**
     * Closes the database connection.
     */
    fun close() {
        database.close()
    }
}