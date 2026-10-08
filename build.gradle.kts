plugins {
    java
}

group = "me.uc.hussein"
version = "v1"

val paperApiVersion: String by project
val javaRelease: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // PlaceholderAPI and Vault are accessed via reflection at runtime, so they are NOT build dependencies.
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease.toInt())
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // Only plugin.yml is filtered; every other resource is copied byte-for-byte.
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("ULTRAS_TAB_V1")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}
