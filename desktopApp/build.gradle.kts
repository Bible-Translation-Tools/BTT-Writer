import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val versionName = libs.versions.app.version.name.get()

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.koin.core)
    implementation(libs.bible.logger)
}

compose.desktop {
    application {
        mainClass = "org.bibletranslationtools.writer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BTT-Writer-2.0"
            packageVersion = versionName
            description = "Bible Translation Tools Writer 2.0"
            copyright = "© 2026 Wycliffe Associates"
            vendor = "WycliffeAssociates"

            modules(
                "java.instrument",
                "java.management",
                "java.prefs",
                "java.rmi",
                "java.security.jgss",
                "java.sql",
                "java.xml.crypto",
                "jdk.security.auth",
                "jdk.unsupported"
            )

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
