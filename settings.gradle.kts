pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://raw.github.com/microsoft/project-rome/mvn-repo/")
    }
}

rootProject.name = "Share"
include(":app")
include(":app:web")
include(":app:shared")
include(":app:wifidirect")
include(":app:msnearshare")
include(":app:lannsd")
include(":app:udptransport")
include(":app:taskMgr")
include(":utils")
