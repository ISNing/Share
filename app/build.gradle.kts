plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "org.exthmui.share"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        applicationId = "org.exthmui.share"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = rootProject.extra["sourceCompatibility"] as JavaVersion
        targetCompatibility = rootProject.extra["targetCompatibility"] as JavaVersion
    }
    kotlinOptions {
        jvmTarget = rootProject.extra["jvmTarget"] as String
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.work.runtime)
    implementation(libs.constraintlayout)
    implementation(libs.percentlayout)
    implementation(libs.preference)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    debugImplementation(libs.leakcanary.android)

    implementation(project(":utils"))

    implementation(project(":app:shared"))
    implementation(project(":app:taskMgr"))
    implementation(project(":app:msnearshare"))
    implementation(project(":app:wifidirect"))
    implementation(project(":app:web"))
    implementation(project(":app:lannsd"))
}