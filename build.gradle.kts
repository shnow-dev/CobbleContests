plugins {
    id("java")
    id("java-library")
    id("dev.architectury.loom") version("1.11-SNAPSHOT") apply false
    id("architectury-plugin") version("3.4-SNAPSHOT") apply false
}

base {
    archivesName = "cobble_contests"
}

allprojects {
    apply(plugin = "java")

    //group = "com.raspix.cobble_contests"
    //version = "1.0.2"
    version = project.properties["mod_version"]!!
    group = project.properties["maven_group"]!!

    repositories {
        mavenCentral()
        maven("https://maven.impactdev.net/repository/development/") {
            content { includeGroup("com.cobblemon") }
        }
        maven("https://maven.neoforged.net/releases") {
            content {
                includeGroupByRegex("net\\.neoforged(\\..*)?")
                includeGroup("cpw.mods")
                includeGroupByRegex("net\\.minecraftforge(\\..*)?")
            }
        }
        maven("https://thedarkcolour.github.io/KotlinForForge/") {
            content { includeGroup("thedarkcolour") }
        }
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

}
