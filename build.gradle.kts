@file:OptIn(ExperimentalAbiValidation::class)

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.report.ReportMergeTask
import groovy.lang.Closure
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.detekt)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin)
  alias(libs.plugins.publish)
}

dependencies {
  compileOnly(libs.detekt.api)
  testImplementation(kotlin("compiler"))
  testImplementation(kotlin("test"))
  testImplementation(libs.assertk)
  testImplementation(libs.detekt.api)
  testImplementation(libs.detekt.test)
  testImplementation(libs.detekt.testJunit)
  testImplementation(libs.detekt.testUtils)
  testImplementation(libs.jetbrains.annotations)
}

val javaVersion: Provider<String> =
  providers.fileContents(layout.projectDirectory.file(".java-version")).asText.map { it.trim() }

val compileTasks = tasks.withType(KotlinCompile::class)

compileTasks.configureEach {
  compilerOptions {
    jvmTarget = javaVersion.map(JvmTarget::fromTarget)
    freeCompilerArgs.addAll("-Xsam-conversions=class", "-Xcontext-sensitive-resolution")
  }
}

tasks.register("compileAll") {
  dependsOn(compileTasks)
}

kotlin {
  explicitApi()
  abiValidation()
}

java {
  val version = JavaVersion.toVersion(javaVersion.get())
  sourceCompatibility = version
  targetCompatibility = version
}

tasks.withType(Test::class).configureEach {
  useJUnitPlatform()
  failOnNoDiscoveredTests = false
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
    exceptionFormat = FULL
    showCauses = true
    showExceptions = true
    showStackTraces = true
    showStandardStreams = false
    displayGranularity = 2
  }
}

detekt {
  config.setFrom(layout.projectDirectory.file("config/detekt.yml"))
  buildUponDefaultConfig = true
  allRules = true
  parallel = true
}

val detektTasks = tasks.withType(Detekt::class)

val detektCheck =
  tasks.register("detektCheck") {
    group = VERIFICATION_GROUP
    dependsOn(detektTasks)
  }

val detektReportMerge =
  tasks.register("detektReportMerge", ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merged.sarif")
    input.from(detektTasks.map { it.reports.sarif.outputLocation })
  }

tasks.named("check").configure {
  dependsOn(detektCheck)
  dependsOn(detektReportMerge)
}

detektTasks.configureEach {
  reports.html.required = true
  reports.sarif.required = true
  exclude { it.file.path.contains("generated") }
}

buildConfig {
  generateAtSync = true
  sourceSets.named("test") {
    packageName = "dev.jonpoulton.detekt.gradle.test"
    useKotlinOutput { topLevelConstants = true }

    buildConfigField(
      name = "GRADLE_JARS",
      value =
        listOf(
            Action::class, // base-services
            Closure::class, // groovy
            DependencyHandlerScope::class, // kotlin-dsl
            Project::class, // core-api
            AntBuilder::class, // ant-api (supertype of Configuration since Gradle 9.5)
          )
          .map { it.java.protectionDomain.codeSource.location.path },
    )
  }
}
