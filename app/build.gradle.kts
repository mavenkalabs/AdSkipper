plugins {
    id("com.android.application")
}

android {
    namespace = "com.mavenkalabs.adskipper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mavenkalabs.adskipper"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "1.0.14"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments += mapOf(
            "clearPackageData" to "true",
        )
    }
    buildTypes {
        debug {
            enableAndroidTestCoverage = false
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment:2.9.1")
    implementation("androidx.navigation:navigation-ui:2.9.1")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.android.support.test:runner:1.0.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.18.0")

    // Core library
    androidTestImplementation("androidx.test:core:1.6.1")

    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestUtil("androidx.test:orchestrator:1.5.1")

    // Assertions
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:truth:1.6.0")

    // uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

}