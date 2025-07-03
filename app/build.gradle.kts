
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")

}

android {
    namespace = "com.example.expensees"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.expensees"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose.android)
    implementation("androidx.core:core:1.13.1")
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation ("io.coil-kt:coil-compose:2.7.0")
    implementation ("androidx.compose.material:material-icons-extended:1.6.8")
    implementation ("androidx.compose.material3:material3:<version>")
    implementation ("androidx.activity:activity-compose:<version>")
    implementation ("androidx.compose:compose-bom:2023.03.00")
    implementation ("androidx.compose.foundation:foundation")
    implementation(libs.glide)
    implementation(libs.glide.compose)
    implementation(libs.coil.compose)
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation ("androidx.navigation:navigation-compose:2.7.7")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.animation:animation:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation ("androidx.compose.material3:material3:1.2.0")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation ("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    implementation ("androidx.compose.runtime:runtime-saveable:1.3.0")
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.google.code.gson:gson:2.11.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("io.insert-koin:koin-androidx-compose:3.5.0")
    implementation ("io.insert-koin:koin-core:3.5.0")
    implementation ("com.auth0:java-jwt:4.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("id.zelory:compressor:3.0.1")
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.27.0")
}