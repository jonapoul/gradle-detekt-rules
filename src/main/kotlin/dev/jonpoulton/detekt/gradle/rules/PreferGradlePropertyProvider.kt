package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags direct Gradle property lookups in favour of `providers.gradleProperty`.
 *
 * `Project.property()`, `Project.findProperty()`, and `Project.hasProperty()` eagerly read property
 * values at configuration time and are not compatible with the configuration cache.
 * `providers.gradleProperty()` returns a lazy `Provider<String>` that defers the read until the
 * value is actually needed, supports configuration cache, and integrates with Gradle's property
 * precedence chain.
 *
 * Noncompliant:
 * ```kotlin
 * val value = property("myProp")
 * val nullable = findProperty("myProp")
 * val exists = hasProperty("myProp")
 * ```
 *
 * Compliant:
 * ```kotlin
 * val value = providers.gradleProperty("myProp")
 * val exists = providers.gradleProperty("myProp").isPresent
 * ```
 */
internal class PreferGradlePropertyProvider(config: Config) :
  Rule(
    config = config,
    description =
      "Prefer providers.gradleProperty over direct property access for configuration cache compatibility",
  ),
  RequiresAnalysisApi {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    val calleeName = expression.calleeExpression?.text ?: return
    if (calleeName !in PROPERTY_METHODS) return

    analyze(expression) {
      val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
      val receiverType = call.dispatchReceiver?.type ?: return@analyze
      if (receiverType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.report(
          "Prefer providers.gradleProperty over $calleeName for lazy, configuration-cache-safe access"
        )
      }
    }
  }

  private companion object {
    val PROPERTY_METHODS = setOf("property", "findProperty", "hasProperty")
  }
}
