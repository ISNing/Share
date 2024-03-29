plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "org.exthmui.share.wifidirect"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = 16

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime)
    implementation(libs.preference)

    implementation(libs.apache.commons.lang3)

    implementation(project(":utils"))

    implementation(project(":app:shared"))
    implementation(project(":app:udptransport"))
}