package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags usages of `Project.afterEvaluate`.
 *
 * `afterEvaluate` breaks project isolation by depending on the evaluation order of projects, and is
 * incompatible with Gradle's configuration cache. Logic placed inside `afterEvaluate` is also
 * harder to reason about and test. Instead, use lazy configuration APIs such as `provider {}`,
 * `tasks.named {}`, or convention plugins to react to project state.
 *
 * Noncompliant:
 * ```kotlin
 * afterEvaluate {
 *     tasks.named("assemble") { dependsOn("myTask") }
 * }
 * ```
 *
 * Compliant:
 * ```kotlin
 * tasks.named("assemble") { dependsOn("myTask") }
 * ```
 */
internal class AvoidAfterEvaluate(config: Config) :
  Rule(
    config,
    "afterEvaluate breaks project isolation and is incompatible with configuration cache",
  ),
  RequiresAnalysisApi {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    if (expression.calleeExpression?.text != AFTER_EVALUATE) return

    analyze(expression) {
      val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
      val receiverType = call.dispatchReceiver?.type ?: return@analyze

      if (receiverType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.report(description)
      }
    }
  }

  private companion object {
    const val AFTER_EVALUATE = "afterEvaluate"
  }
}
