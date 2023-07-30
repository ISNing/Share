plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "org.exthmui.share.msnearshare"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = 1

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
    implementation(libs.work.runtime)
    implementation(libs.preference)

    implementation(project(":utils"))

    implementation(project(":app:shared"))

    // Never upgrade it unless microsoft re-maintained Project-Rome
    // Because there's a serious bug in newer version but Project-Rome seems not in maintaining.
    // See: https://github.com/microsoft/project-rome/issues/85
    implementation("com.microsoft.connecteddevices:connecteddevices-sdk:1.3.0")
}