import org.gradle.process.CommandLineArgumentProvider

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.android.compose.screenshot") version "0.0.1-alpha16"
}

// Compile the actual app sources and resources; no production module configuration is modified.
val appSource = rootDir.resolve("../../app").canonicalFile

android {
    namespace = "com.example.drivingcoach"
    useLibrary("android.car", false)
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.example.drivingcoach"
        minSdk = 28
        targetSdk = 37
    }
    buildFeatures { compose = true }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets.getByName("main") {
        manifest.srcFile(appSource.resolve("src/main/AndroidManifest.xml"))
        java.directories += appSource.resolve("src/main/java").absolutePath
        kotlin.directories += appSource.resolve("src/main/java").absolutePath
        res.directories += appSource.resolve("src/main/res").absolutePath
        assets.directories += appSource.resolve("src/main/assets").absolutePath
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}

// The alpha16 plugin writes UTF-8 properties; its engine loads them as ISO-8859-1.
// Preserve non-ASCII workspace paths using standard Java-properties Unicode escapes.
tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        val config = temporaryDir.resolve("preview_screenshot_config.properties")
        if (config.isFile) {
            val escaped = config.readText(Charsets.UTF_8).map { character ->
                if (character.code > 127) "\\u%04x".format(character.code) else character.toString()
            }.joinToString("")
            config.writeText(escaped, Charsets.US_ASCII)
        }
        emptyList()
    })
}
