package org.bibletranslationtools.writer.di

import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.ArchiveImporter
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.bibletranslationtools.writer.core.BackupRunner
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPrefOrNull
import org.bibletranslationtools.writer.git.TransportCallback
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.usecases.AdvancedGogsRepoSearch
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.CloneRepository
import org.bibletranslationtools.writer.usecases.CreateRepository
import org.bibletranslationtools.writer.usecases.DownloadImages
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.usecases.GetAvailableSources
import org.bibletranslationtools.writer.usecases.GetRepository
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.ImportDraft
import org.bibletranslationtools.writer.usecases.ImportIndex
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.MergeTargetTranslation
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.bibletranslationtools.writer.usecases.PullTargetTranslation
import org.bibletranslationtools.writer.usecases.PushTargetTranslation
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.bibletranslationtools.writer.usecases.RenderHelps
import org.bibletranslationtools.writer.usecases.SearchGogsRepositories
import org.bibletranslationtools.writer.usecases.SearchGogsUsers
import org.bibletranslationtools.writer.usecases.TranslationProgress
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.bibletranslationtools.writer.usecases.UpdateCatalogs
import org.bibletranslationtools.writer.usecases.UpdateSource
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.bibletranslationtools.writer.usecases.ValidateProject
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    singleOf(::BackupRunner)
    singleOf(::BackupRC)
    singleOf(::Translator)
    singleOf(::ArchiveImporter)
    singleOf(::TargetTranslationMigrator)
    singleOf(::ArchiveMigrator)

    single<Profile> {
        val pref: Preference = get()
        val dir: DirectoryProvider = get()
        try {
            val profileString = pref.getPrefOrNull<String>(Preference.PROFILE)
            Profile.fromJSON(pref, dir, profileString)
        } catch (e: Exception) {
            throw e
        }
    }

    singleOf(::UpdateSource)
    singleOf(::PushTargetTranslation)
    singleOf(::GetRepository)
    singleOf(::CreateRepository)
    singleOf(::SearchGogsRepositories)
    singleOf(::SearchGogsUsers)
    singleOf(::AdvancedGogsRepoSearch)
    singleOf(::GogsLogin)
    singleOf(::PullTargetTranslation)
    singleOf(::UploadFeedback)
    singleOf(::ImportProjects)
    singleOf(::RegisterSSHKeys)
    singleOf(::ExportProjects)
    singleOf(::ImportDraft)
    singleOf(::UpdateCatalogs)
    singleOf(::TranslationProgress)
    singleOf(::CheckForLatestRelease)
    singleOf(::UploadCrashReport)
    singleOf(::CloneRepository)
    singleOf(::ImportIndex)
    singleOf(::MergeTargetTranslation)
    singleOf(::DownloadResourceContainers)
    singleOf(::MigrateTranslations)
    singleOf(::GogsLogout)
    singleOf(::RenderHelps)
    singleOf(::ValidateProject)
    singleOf(::GetAvailableSources)
    singleOf(::RenderingProvider)

    singleOf(::Typography)
    singleOf(::DownloadImages)
    singleOf(::UpdateApp)
    single {
        val directoryProvider: DirectoryProvider = get()
        ResourceCatalogClient(
            directoryProvider.databaseFile,
            directoryProvider.containersDir
        )
    }

    singleOf(::Preference)
    singleOf(::TransportCallback)
    singleOf(::ProcessUSFM)
}