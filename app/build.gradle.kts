import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
}

android {
    namespace = "com.mavenkalabs.adskipper"
    compileSdk = 37

    defaultConfig {
        testInstrumentationRunnerArguments += mapOf("clearPackageData" to "true")
        applicationId = "com.mavenkalabs.adskipper"
        minSdk = 24
        targetSdk = 37
        versionCode = 22
        versionName = "1.0.22"

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
            isMinifyEnabled = true
            isShrinkResources = true
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.navigation:navigation-fragment:2.10.2")
    implementation("androidx.navigation:navigation-ui:2.10.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.annotation:annotation:1.11.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.fragment:fragment:1.9.1")
    implementation("androidx.core:core-ktx:1.19.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
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
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
}