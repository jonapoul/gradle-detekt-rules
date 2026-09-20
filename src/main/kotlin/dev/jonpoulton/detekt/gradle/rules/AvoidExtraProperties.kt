package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Flags accesses to `Project.extra` (extra properties).
 *
 * Extra properties are eager, untyped, and not compatible with the configuration cache. They couple
 * build scripts together through shared mutable state, making builds harder to reason about and
 * breaking project isolation. Prefer typed extensions, shared build services, convention plugins,
 * or `providers.gradleProperty` to share configuration between projects.
 *
 * Noncompliant:
 * ```kotlin
 * extra["myKey"] = "value"
 * val value = extra["myKey"]
 * extra.set("myKey", "value")
 * ```
 *
 * Compliant:
 * ```kotlin
 * // Use a typed extension on the project
 * extensions.create<MyExtension>("myExtension")
 * // Or use providers.gradleProperty for simple key-value properties
 * val value = providers.gradleProperty("myKey")
 * ```
 */
internal class AvoidExtraProperties(config: Config) :
  Rule(
    config = config,
    description =
      "Extra properties are eager and untyped - prefer typed extensions or convention plugins",
  ),
  RequiresAnalysisApi {
  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    if (expression.getReferencedName() != EXTRA) return

    analyze(expression) {
      val call =
        expression.resolveToCall()?.singleCallOrNull<KaVariableAccessCall>() ?: return@analyze
      val receiverType =
        call.dispatchReceiver?.type ?: call.extensionReceiver?.type ?: return@analyze
      if (receiverType.isSubtypeOf(GradleExtensionAware)) {
        expression.report(description)
      }
    }
  }

  private companion object {
    const val EXTRA = "extra"
    val GradleExtensionAware = FqName("org.gradle.api.plugins.ExtensionAware")
  }
}
