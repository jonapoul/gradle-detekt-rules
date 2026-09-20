package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags `subprojects { }` and `allprojects { }` calls that break project isolation.
 *
 * These methods configure other projects from the current project, creating implicit coupling
 * between projects that prevents Gradle from isolating project configuration. This breaks the
 * configuration cache, prevents parallel project configuration, and makes builds harder to reason
 * about. Use convention plugins applied in each project's own `build.gradle.kts` instead.
 *
 * Noncompliant:
 * ```kotlin
 * subprojects {
 *     apply(plugin = "java-library")
 * }
 * allprojects {
 *     group = "com.example"
 * }
 * ```
 *
 * Compliant:
 * ```kotlin
 * // In build-logic/src/main/kotlin/my-convention.gradle.kts
 * plugins { id("java-library") }
 * group = "com.example"
 *
 * // In each subproject's build.gradle.kts
 * plugins { id("my-convention") }
 * ```
 */
internal class AvoidCrossProjectConfiguration(config: Config) :
  Rule(
    config = config,
    description =
      "subprojects/allprojects break project isolation - use convention plugins instead",
  ),
  RequiresAnalysisApi {
  @Configuration("Allow allprojects blocks, e.g. for repository declarations in the root project")
  private val allowAllprojects: Boolean by config(defaultValue = false)

  private val activeMethods: Set<String>
    get() = if (allowAllprojects) CROSS_PROJECT_METHODS - "allprojects" else CROSS_PROJECT_METHODS

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    val calleeName = expression.calleeExpression?.text ?: return
    if (calleeName !in activeMethods) return

    analyze(expression) {
      val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
      val receiverType = call.dispatchReceiver?.type ?: return@analyze
      if (receiverType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.report("$calleeName breaks project isolation - use convention plugins instead")
      }
    }
  }

  private companion object {
    val CROSS_PROJECT_METHODS = setOf("subprojects", "allprojects")
  }
}
