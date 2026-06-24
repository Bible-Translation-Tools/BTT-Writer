import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

val versionName = libs.versions.app.version.name.get()
val versionCode = libs.versions.app.version.code.get()

val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
    val versionNameValue = versionName
    val versionCodeValue = versionCode

    inputs.property("versionName", versionNameValue)
    inputs.property("versionCode", versionCodeValue)
    outputs.dir(outputDir)

    doLast {
        val pkgDir = outputDir.get().asFile.resolve("org/bibletranslationtools/writer")
        pkgDir.mkdirs()
        pkgDir.resolve("BuildInfo.kt").writeText(
            """
            package org.bibletranslationtools.writer

            internal object BuildInfo {
                const val VERSION_NAME = "$versionNameValue"
                const val VERSION_CODE = "$versionCodeValue"
                const val OAUTH_TOKEN = "bad_token"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "org.bibletranslationtools.writer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        packaging {
            resources {
                pickFirsts.add("plugin.properties")
                pickFirsts.add("META-INF/DEPENDENCIES")
                pickFirsts.add("META-INF/LICENSE*")
                pickFirsts.add("META-INF/NOTICE*")
            }
        }
    }

    jvm()

    sourceSets {
        androidMain {
            dependencies {
                // Koin
                implementation(libs.koin.android)
            }
        }

        commonMain {
            kotlin.srcDir(generateBuildInfo)

            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.material.icons.extended)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.io)
                implementation(libs.kotlinx.serialization.json)

                // JGit
                implementation(libs.jgit)
                implementation(libs.jgit.ssh.apache)
                implementation(libs.bcprov.jdk18on)
                // Provides the missing javax.management classes for jgit Android
                // We can remove this only with minSdk = 33 and jgit 6+
                implementation(libs.jmx)

                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                // Decompose
                api(libs.decompose)
                implementation(libs.decompose.extensions.compose)

                // Bible Translation Tools
                implementation(libs.resource.container)
                implementation(libs.gogs.client)
                implementation(libs.resource.catalog.client)
                implementation(libs.bible.logger)

                // PDF
                implementation(libs.itextg)

                // HTML
                implementation(libs.html.converter)

                // Settings
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.datastore)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.datastore.preferences.core)

                // FileKit
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.koin.test)

                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.mockk.agent)
                implementation(libs.mock.webserver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.oshi.core)
            }
        }
    }
}

configurations {
    configureEach {
        exclude(module = "sshd-core")
        exclude(module = "sshd-common")
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
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

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBuildInfo)
}

tasks.named<Test>("jvmTest") {
    testLogging {
        events("passed", "skipped", "failed")

        displayGranularity = 2
        showExceptions = true
        exceptionFormat = TestExceptionFormat.SHORT
    }
}
