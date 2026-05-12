import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val bamlSdkNative by configurations.creating

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

val demoApiKey = (
    localProperties.getProperty("OPENROUTER_API_KEY")
        ?: localProperties.getProperty("BAML_OPENROUTER_API_KEY")
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: System.getenv("BAML_OPENROUTER_API_KEY")
        ?: ""
).escapeForBuildConfig()

android {
    namespace = "com.example.kitchenrecipeappbaml"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.kitchenrecipeappbaml"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BAML_OPENROUTER_API_KEY", "\"$demoApiKey\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    sourceSets["main"].jniLibs.setSrcDirs(
        listOf(layout.buildDirectory.dir("generated/baml-jniLibs").get().asFile)
    )
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation("io.github.ravitejguntuku:baml-kotlin:0.1.0") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    bamlSdkNative("io.github.ravitejguntuku:baml-kotlin:0.1.0")
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

val syncBamlBridgeLibs by tasks.registering(Sync::class) {
    group = "build"
    description = "Extract Android bridge_cffi libraries from the baml-kotlin SDK jar"

    val sdkJar = bamlSdkNative.incoming.artifactView { }.files.elements.map { files ->
        files
            .map { it.asFile }
            .single { it.name.startsWith("baml-kotlin-") && it.extension == "jar" }
    }

    from(sdkJar.map { zipTree(it) }) {
        include("native/android-arm64/libbridge_cffi.so")
        eachFile {
            path = "arm64-v8a/${name}"
        }
        includeEmptyDirs = false
    }

    from(sdkJar.map { zipTree(it) }) {
        include("native/android-x86_64/libbridge_cffi.so")
        eachFile {
            path = "x86_64/${name}"
        }
        includeEmptyDirs = false
    }

    into(layout.buildDirectory.dir("generated/baml-jniLibs"))
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn(syncBamlBridgeLibs)
}

tasks.matching { it.name.startsWith("configureCMake") || it.name.startsWith("buildCMake") }.configureEach {
    dependsOn(syncBamlBridgeLibs)
}
