package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease.Release

class DownloadLatestRelease(
    private val platform: Platform
) {
    fun execute(release: Release) {
//        if (platform.isStoreVersion) {
//            // open play store
//            val appPackageName: String = context.packageName
//            try {
//                val intent = Intent(
//                    Intent.ACTION_VIEW,
//                    "market://details?id=$appPackageName".toUri()
//                )
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//
//                context.startActivity(intent)
//            } catch (_: ActivityNotFoundException) {
//                val intent = Intent(
//                    Intent.ACTION_VIEW,
//                    "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
//                )
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                context.startActivity(intent)
//            }
//        } else {
//            // download from GitHub
//            val browserIntent = Intent(Intent.ACTION_VIEW, release.downloadUrl.toUri())
//            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            context.startActivity(browserIntent)
//        }
    }
}