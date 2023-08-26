plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.exthmui.share.taskMgr"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
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
        sourceCompatibility = rootProject.extra["sourceCompatibility"] as JavaVersion
        targetCompatibility = rootProject.extra["targetCompatibility"] as JavaVersion
    }
    kotlinOptions {
        jvmTarget = rootProject.extra["jvmTarget"] as String
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.core.ktx)

    implementation(libs.lifecycle.livedata.core)
    implementation(libs.lifecycle.process)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(project(":utils"))
}