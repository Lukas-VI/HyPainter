plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.lukasvi.hypainter"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.lukasvi.hypainter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(layout.buildDirectory.dir("generated/rustJniLibs"))
        }
    }
}

val buildRustArm64Debug by tasks.registering(Exec::class) {
    workingDir = rootDir
    commandLine(
        "powershell",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "${rootDir}/scripts/build-rust-android.ps1",
        "-Target",
        "aarch64-linux-android",
        "-Abi",
        "arm64-v8a",
        "-OutputDir",
        "${layout.buildDirectory.get().asFile}/generated/rustJniLibs/arm64-v8a",
    )
}

tasks.matching { it.name == "mergeDebugJniLibFolders" }.configureEach {
    dependsOn(buildRustArm64Debug)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("io.github.alex3236:lucide-compose:1.0.0-alpha01")
}
