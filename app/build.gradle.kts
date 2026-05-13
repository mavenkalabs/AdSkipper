plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mavenkalabs.adskipper"
    compileSdk = 36

    defaultConfig {
        testInstrumentationRunnerArguments += mapOf("clearPackageData" to "true")
        applicationId = "com.mavenkalabs.adskipper"
        minSdk = 24
        targetSdk = 36
        versionCode = 19
        versionName = "1.0.19"

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
        sourceCompatibility = JavaVersion.VERSION_1_9
        targetCompatibility = JavaVersion.VERSION_1_9
    }
    buildFeatures {
        viewBinding = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_9)
        }
    }
    kotlinOptions {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_9
            targetCompatibility = JavaVersion.VERSION_1_9
        }
    }
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
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20251224")
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
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
}