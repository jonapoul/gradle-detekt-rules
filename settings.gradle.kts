@file:Suppress("UnstableApiUsage")

rootProject.name = "gradle-detekt-rules"

pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("com.gradle.develocity") version "4.5.1"
  id("io.github.gmazzo.publications.report") version "1.4.1"
}

develocity {
  buildScan.publishing.onlyIf { false }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
