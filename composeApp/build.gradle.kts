import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "org.bibletranslationtools.writer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
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
                // Provides the missing javax.management classes for Android
                //implementation(libs.jmx)

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

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.koin.test)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
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
            packageName = "org.bibletranslationtools.writer"
            packageVersion = "1.0.0"
        }
    }
}
