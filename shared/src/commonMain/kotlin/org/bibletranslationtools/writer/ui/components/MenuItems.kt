package org.bibletranslationtools.writer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_search
import btt_writer.shared.generated.resources.action_settings
import btt_writer.shared.generated.resources.action_translations
import btt_writer.shared.generated.resources.feedback
import btt_writer.shared.generated.resources.label_import_options
import btt_writer.shared.generated.resources.log_out
import btt_writer.shared.generated.resources.mark_chunks_done
import btt_writer.shared.generated.resources.menu_update_library
import btt_writer.shared.generated.resources.menu_upload_export
import btt_writer.shared.generated.resources.print
import btt_writer.shared.generated.resources.share_apk
import btt_writer.shared.generated.resources.title_review
import btt_writer.shared.generated.resources.view_available_drafts
import org.bibletranslationtools.writer.core.TranslationViewMode
import org.jetbrains.compose.resources.stringResource

data class SidebarAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun rememberTranslateMenuItems(
    viewMode: TranslationViewMode,
    draftAvailable: Boolean,
    onHomeClick: () -> Unit,
    onNavigateToDraft: () -> Unit,
    onProjectPreview: () -> Unit,
    onUploadExport: () -> Unit,
    onPrint: () -> Unit,
    onFeedback: () -> Unit,
    onChunksDone: () -> Unit,
    onSettings: () -> Unit,
    onSearchRequested: () -> Unit,
    isStandardProject: Boolean = true
): List<SidebarAction> {
    val translations = stringResource(Res.string.action_translations)
    val viewDrafts = stringResource(Res.string.view_available_drafts)
    val review = stringResource(Res.string.title_review)
    val uploadExport = stringResource(Res.string.menu_upload_export)
    val print = stringResource(Res.string.print)
    val feedback = stringResource(Res.string.feedback)
    val search = stringResource(Res.string.action_search)
    val markDone = stringResource(Res.string.mark_chunks_done)
    val settings = stringResource(Res.string.action_settings)

    return remember(viewMode, draftAvailable, isStandardProject) {
        buildList {
            add(
                SidebarAction(translations, Icons.AutoMirrored.Filled.LibraryBooks, onHomeClick)
            )
            if (draftAvailable) {
                add(
                    SidebarAction(viewDrafts, Icons.Default.Translate, onNavigateToDraft)
                )
            }
            add(
                SidebarAction(review, Icons.Default.DoneAll, onProjectPreview)
            )
            add(
                SidebarAction(uploadExport, Icons.Default.Upload, onUploadExport)
            )
            if (isStandardProject) {
                add(
                    SidebarAction(print, Icons.Default.Print, onPrint)
                )
            }
            add(
                SidebarAction(feedback, Icons.Default.Feedback, onFeedback)
            )
            if (viewMode == TranslationViewMode.REVIEW) {
                if (isStandardProject) {
                    add(
                        SidebarAction(search, Icons.Default.Search, onSearchRequested)
                    )
                }
                add(
                    SidebarAction(markDone, Icons.Default.Check, onChunksDone)
                )
            }
            add(
                SidebarAction(settings, Icons.Default.Settings, onSettings)
            )
        }
    }
}

@Composable
fun rememberHomeMenuItems(
    onUpdateClick: () -> Unit,
    onImport: () -> Unit,
    onFeedback: () -> Unit,
    onShareApp: () -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit
): List<SidebarAction> {
    val update = stringResource(Res.string.menu_update_library)
    val import = stringResource(Res.string.label_import_options)
    val feedback = stringResource(Res.string.feedback)
    val shareApp = stringResource(Res.string.share_apk)
    val logout = stringResource(Res.string.log_out)
    val settings = stringResource(Res.string.action_settings)

    return remember {
        buildList {
            add(
                SidebarAction(update, Icons.Default.LocalLibrary, onUpdateClick)
            )
            add(
                SidebarAction(import, Icons.Default.Download, onImport)
            )
            add(
                SidebarAction(feedback, Icons.Default.Feedback, onFeedback)
            )
            add(
                SidebarAction(shareApp, Icons.Default.Android, onShareApp)
            )
            add(
                SidebarAction(logout, Icons.AutoMirrored.Filled.ExitToApp, onLogout)
            )
            add(
                SidebarAction(settings, Icons.Default.Settings, onSettings)
            )
        }
    }
}
