@file:Suppress("UnusedReceiverParameter")

package dev.jonpoulton.detekt.gradle.rules

import assertk.Assert
import assertk.assertions.isNotNull
import assertk.assertions.support.expected
import assertk.assertions.support.show
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.utils.KotlinAnalysisApiEngine
import dev.detekt.test.utils.KotlinEnvironmentContainer
import dev.jonpoulton.detekt.gradle.test.GRADLE_JARS
import kotlin.io.path.Path
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

internal fun <T> Assert<T>.lintedAsKts(
  environment: KotlinEnvironmentContainer,
  @Language("kotlin") script: String,
) where T : Rule, T : RequiresAnalysisApi = transform { rule ->
  val codeWithImports =
    """
    import org.gradle.api.*
    import org.gradle.api.artifacts.*
    import org.gradle.api.plugins.*
    import org.gradle.api.provider.*
    import org.gradle.api.tasks.*
    import org.gradle.kotlin.dsl.*

    fun Project.script() {
      $script
    }
  """
      .trimIndent()

  // recreating rule.lintWithContext with custom JAR paths
  KotlinAnalysisApiEngine().use { engine ->
    val ktFile =
      engine.compile(
        code = codeWithImports,
        javaSourceRoots = environment.javaSourceRoots,
        jvmClasspathRoots = environment.jvmClasspathRoots + GRADLE_JARS.map(::Path),
        allowCompilationErrors = false,
      )
    rule.visitFile(ktFile, LanguageVersionSettingsImpl.DEFAULT)
  }
}

internal fun Assert<List<Finding>>.hasNoFindings() = transform { findings ->
  if (!findings.isEmpty()) expected("to be empty but was:${show(findings)}")
  findings
}

internal fun Assert<List<Finding>>.hasNumFindings(expected: Int) = transform { findings ->
  if (findings.size != expected) {
    expected("to have $expected but had ${findings.size}: ${show(findings)}")
  }
  findings
}

internal fun Assert<List<Finding>>.hasOneFinding() =
  hasNumFindings(expected = 1).transform { it[0] }

internal fun Assert<List<Finding>>.hasTwoFindings() = hasNumFindings(expected = 2)

internal fun Assert<List<Finding>>.hasThreeFindings() = hasNumFindings(expected = 3)

internal fun Assert<List<Finding>>.onFirstFinding(block: Assert<Finding>.() -> Unit) =
  onFinding(number = 1, block)

internal fun Assert<List<Finding>>.onSecondFinding(block: Assert<Finding>.() -> Unit) =
  onFinding(number = 2, block)

internal fun Assert<List<Finding>>.onThirdFinding(block: Assert<Finding>.() -> Unit) =
  onFinding(number = 3, block)

internal fun Assert<List<Finding>>.onFinding(number: Int, block: Assert<Finding>.() -> Unit) =
  transform { findings ->
    assertThat(findings.getOrNull(index = number - 1), name = "finding $number").isNotNull().block()
    findings
  }

internal fun Assert<Finding>.messageContains(expected: String) = transform { finding ->
  if (expected !in finding.message) {
    expected(
      "to have message containing '$expected', but had '${finding.message}': ${show(finding)}"
    )
  }
  finding
}

internal fun Assert<Finding>.withMessage(expected: String) = transform { finding ->
  if (finding.message != expected) {
    expected("to have message '$expected', but had '${finding.message}': ${show(finding)}")
  }
  finding
}
