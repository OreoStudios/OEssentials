pluginManagement {
    repositories {
        maven("https://maven.oreostudios.fr/oreostudioslib") {
            content { includeGroupByRegex("dev\\.oreo.*") }
        }
        gradlePluginPortal()
    }
}

plugins {
    // Lets Gradle auto-download the Java 21 toolchain when it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "OEssentials"
