rootProject.name = "CobbleContests"

pluginManagement {
    repositories {
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/") {
            content {
                includeGroupByRegex("net\\.fabricmc(\\..*)?")
            }
        }
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
}

listOf(
    "common",
    "neoforge"
).forEach { include(it)}
