plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")

    //id("eclipse")
    //id("idea")
    //id("maven-publish")
}

architectury {
    common("neoforge")
    //platformSetupLoomIde()
}

loom {
    silentMojangMappingsLicense()
}

base {
    archivesName = "cobble_contests"
}

dependencies {

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("com.cobblemon:mod:${property("cobblemon_version")}") {
        isTransitive = false
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit_version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit_version")}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
