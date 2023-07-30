//buildscript {
//    repositories {
//        mavenCentral()
//    }
//    dependencies {
//        classpath("io.github.isning.andserver:plugin:3.0.0")
//    }
//}

plugins {
    alias(libs.plugins.androidLibrary)
//    id("com.yanzhenjie.andserver") version "3.0.0"
}

android {
    namespace = "org.exthmui.share.web"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = rootProject.extra["sourceCompatibility"] as JavaVersion
        targetCompatibility = rootProject.extra["targetCompatibility"] as JavaVersion
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.constraintlayout)

    implementation(libs.gson)

    implementation(project(":utils"))

    implementation(project(":app:shared"))

    implementation("io.github.isning.andserver:api:3.0.0")
//    annotationProcessor("io.github.isning.andserver:processor:3.0.0")
}