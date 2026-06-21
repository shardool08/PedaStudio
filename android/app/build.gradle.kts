plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.tippingpoint.pedastudio"
    compileSdk = 35

    val localProps = rootProject.file("local.properties")
    val gradleProps = rootProject.file("gradle.properties")

    fun readProp(file: java.io.File, key: String): String {
        if (!file.exists()) return ""
        val raw = file.readLines()
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?: ""
        // Recover from accidental duplicate key, e.g. pedastudio.api.url=http://...
        return if (raw.startsWith("$key=")) raw.substringAfter("=") else raw
    }

    val localApiUrl = readProp(localProps, "pedastudio.api.url")
    val productionApiUrl = readProp(gradleProps, "pedastudio.api.url.production")

    defaultConfig {
        applicationId = "com.tippingpoint.PedaStudio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            val debugUrl = localApiUrl.ifBlank { productionApiUrl }
            buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
        }
        release {
            val releaseUrl = productionApiUrl.ifBlank { localApiUrl }
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.razorpay:checkout:1.6.40")
}
