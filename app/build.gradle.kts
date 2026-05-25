import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    kotlin("plugin.serialization") version "2.0.21" // Or your Kotlin version
}

extensions.configure<ApplicationExtension> {
    namespace = "com.mavenkalabs.adskipper"
    compileSdk = 37

    defaultConfig {
        testInstrumentationRunnerArguments += mapOf("clearPackageData" to "true")
        applicationId = "com.mavenkalabs.adskipper"
        minSdk = 24
        versionCode = 21
        versionName = "1.0.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            enableAndroidTestCoverage = false
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isReturnDefaultValues = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}


kotlin {
    jvmToolchain(17) // Gradle 9 requires at least JDK 17 to run
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment:2.9.8")
    implementation("androidx.navigation:navigation-ui:2.9.8")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("com.android.support.test:runner:1.0.2")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260522")
    // Core library
    androidTestImplementation("androidx.test:core:1.7.0")

    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestUtil("androidx.test:orchestrator:1.6.1")

    // Assertions
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.ext:truth:1.7.0")

    // uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.21")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
}