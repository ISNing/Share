buildscript {
    extra.apply {
        set("compileSdkVersion", 33)
        set("minSdkVersion", 24)
        set("targetSdkVersion", this["compileSdkVersion"])
        set("versionCode", 1)
        set("versionName", "1.0")
        set("sourceCompatibility", JavaVersion.VERSION_1_8)
        set("targetCompatibility", JavaVersion.VERSION_1_8)
        set("jvmTarget", "1.8")
    }
}