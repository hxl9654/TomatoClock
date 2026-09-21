import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

configure<ApplicationExtension> {
    namespace = "com.hxlxz.tomatoclock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hxlxz.tomatoclock"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.hxlxz.tomatoclock.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    installation {
        installOptions.add("-g")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("user.language", "zh")
            it.systemProperty("user.country", "CN")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

roborazzi {
    outputDir.set(file("src/test/roborazzi"))
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation("androidx.navigation:navigation-compose:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    
    // Local Unit Tests (Robolectric & Compose)
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.74.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.74.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.74.0")

    // Instrumented Tests
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.60.1")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    jvmToolchain(17)
}
