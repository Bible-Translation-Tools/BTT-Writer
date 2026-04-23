import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("Door43Database") {
            packageName.set("org.unfoldingword.door43client.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.2.1")
        }
    }
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "org.bibletranslationtools.writer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
    }

    jvm("desktop")

    sourceSets {
        val javaMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.jgit)
                implementation(libs.jgit.ssh.jsch)
                implementation(libs.jsch)
            }
        }

        val desktopMain by getting {
            dependsOn(javaMain)
        }

        val androidMain by getting {
            dependsOn(javaMain)

            dependencies {
                // SQLDelight
                implementation(libs.sqldelight.android)

                // Koin
                implementation(libs.koin.android)
            }
        }

        commonMain.dependencies {
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

            // SqlDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Bible Translation Tools
            implementation(libs.resource.container)
            //implementation(libs.gogs.client)
            implementation(libs.bible.logger)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // SQLDelight Desktop
            implementation(libs.sqldelight.sqlite)
        }
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
