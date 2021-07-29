/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build and run tests") {
    container(displayName = "Run gradle build", image = "openjdk:11") {
        kotlinScript { api ->
            // here can be your complex logic
            api.gradlew("build")
        }
    }
}
