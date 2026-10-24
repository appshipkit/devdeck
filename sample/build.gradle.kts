import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appVersion: String = providers.gradleProperty("VERSION_NAME").get()

fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = version.substringBefore('-').split('.').map(String::toInt)
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "io.devdeck.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.devdeck.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
    }

    buildTypes {
        // A minified, installable build with DevDeck in it, like the internal QA builds apps ship
        // to testers. CI builds it so R8 problems with DevDeck's consumer rules show up here first.
        create("qa") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".qa"
            matchingFallbacks += "release"
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = false
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":devdeck"))
    implementation(project(":devdeck-okhttp"))
    implementation(project(":devdeck-timber"))
    implementation(libs.timber)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}
