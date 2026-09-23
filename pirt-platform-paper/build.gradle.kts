plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":pirt-core"))
    implementation(project(":pirt-integration-worldguard"))
    implementation(project(":pirt-integration-placeholderapi"))
    implementation("org.incendo:cloud-paper:2.0.0")
    implementation("org.incendo:cloud-annotations:2.0.0")
    annotationProcessor("org.incendo:cloud-annotations:2.0.0")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("PIRT-${project.version}.jar")

        relocate("org.incendo.cloud", "dev.darkblade.pirt.libs.cloud")
        relocate("io.leangen.geantyref", "dev.darkblade.pirt.libs.geantyref")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching(listOf("paper-plugin.yml", "plugin.yml")) {
            expand(props)
        }
    }
}
