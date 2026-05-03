plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "org.bibletranslationtools.writer.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.bibletranslationtools.writer"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"

            pickFirsts.add("plugin.properties")
            pickFirsts.add("META-INF/DEPENDENCIES")
            pickFirsts.add("META-INF/LICENSE*")
            pickFirsts.add("META-INF/NOTICE*")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.foundation)
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.uiTooling)

    implementation(libs.koin.android)

    implementation(libs.resource.catalog.client)
    implementation(libs.bible.logger)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
}
