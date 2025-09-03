plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.cash.paparazzi)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.gms.google.services)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dev.ansh.v2fitness"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            // For production, you should use a proper keystore
            // storeFile = file("release.keystore")
            // storePassword = System.getenv("KEYSTORE_PASSWORD")
            // keyAlias = System.getenv("KEY_ALIAS")
            // keyPassword = System.getenv("KEY_PASSWORD")
            
            // For now, using debug keystore - replace with proper release keystore
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "V2Fitness Debug")
            buildConfigField("boolean", "ENABLE_TESTING_MENU", "true")
            buildConfigField("String", "BASE_URL", "\"https://dev-api.v2fitness.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // Production build - no suffix
            resValue("string", "app_name", "V2Fitness")
            buildConfigField("boolean", "ENABLE_TESTING_MENU", "false")
            buildConfigField("String", "BASE_URL", "\"https://api.v2fitness.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Configure variant output file names
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "V2Fitness-${variant.buildType.name}-${variant.versionName}.apk"
                output.outputFileName = outputFileName
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Disable baseline profiles to prevent installation issues
            excludes += "META-INF/com/android/build/gradle/app-metadata.properties"
            excludes += "META-INF/**.prof"
            excludes += "META-INF/profileinstaller/**"
        }
    }

    namespace = "v2"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.bundles.androidx.xr)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.square.moshi.kotlin)
    implementation(libs.square.okhttp.logging.interceptor)
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.converter.moshi)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.accompanist.permissions)
    implementation(libs.biometric)
    implementation("com.google.guava:guava:31.1-android")

    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.square.leakcanary)

    annotationProcessor(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.compose.ui.test.junit)
    androidTestImplementation(libs.hilt.android.testing)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.square.moshi.kotlin.codegen)

    kspAndroidTest(libs.hilt.android.compiler)
}

tasks.formatKotlinMain {
    exclude { it.file.path.contains("build/")}
}

tasks.lintKotlinMain {
    exclude { it.file.path.contains("build/")}
}

// Task to list all available build variants
tasks.register("listVariants") {
    doLast {
        println("\n=== Available Build Variants ===")
        println("Debug Variant:")
        println("  • debug    -> com.dev.ansh.v2fitness.debug")
        println("Release Variant:")
        println("  • release  -> com.dev.ansh.v2fitness")
        println("\n=== Build Commands ===")
        println("./gradlew assembleDebug")
        println("./gradlew assembleRelease")
        println("\n=== Install Commands ===")
        println("./gradlew installDebug")
        println("./gradlew installRelease")
        println("================================\n")
    }
}
