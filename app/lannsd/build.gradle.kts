plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "org.exthmui.share.lannsd"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = 24

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
    kotlinOptions {
        jvmTarget = rootProject.extra["jvmTarget"] as String
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.process)
    implementation(libs.preference)

    implementation(libs.gson)
    implementation(libs.apache.commons.lang3)

    implementation(project(":utils"))

    implementation(project(":app:shared"))
    implementation(project(":app:taskMgr"))
    implementation(project(":app:udptransport"))
}