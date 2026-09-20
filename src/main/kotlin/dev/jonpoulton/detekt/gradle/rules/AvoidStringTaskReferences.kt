package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Flags usages of task wiring methods (`dependsOn`, `mustRunAfter`, `shouldRunAfter`,
 * `finalizedBy`) that are called with string task names instead of `TaskProvider` references.
 *
 * Using string task names creates implicit dependencies that bypass Gradle's task avoidance and
 * configuration cache mechanisms. Passing `TaskProvider` references instead ensures type safety and
 * enables Gradle to properly track task dependencies without eagerly realising tasks.
 *
 * Noncompliant:
 * ```kotlin
 * tasks.register("myTask") {
 *   dependsOn("otherTask")
 *   mustRunAfter("setup")
 *   shouldRunAfter("cleanup")
 *   finalizedBy("report")
 * }
 * ```
 *
 * Compliant:
 * ```kotlin
 * val otherTask = tasks.register("otherTask")
 * tasks.register("myTask") {
 *   dependsOn(otherTask)
 *   mustRunAfter(setupTask)
 *   shouldRunAfter(cleanupTask)
 *   finalizedBy(reportTask)
 * }
 * ```
 */
internal class AvoidStringTaskReferences(config: Config) :
  Rule(
    config = config,
    description =
      "Task wiring methods should reference TaskProviders rather than string task names",
  ),
  RequiresAnalysisApi {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    val calleeName = expression.calleeExpression?.text ?: return
    if (calleeName !in WIRING_METHODS) return
    if (!expression.hasStringArgument()) return

    val parent = expression.parent
    if (parent is KtDotQualifiedExpression) {
      analyze(parent) {
        val receiverType = parent.receiverExpression.expressionType ?: return@analyze
        if (receiverType.isSubtypeOf(Task)) {
          expression.report("Use a TaskProvider instead of a string in $calleeName()")
        }
      }
    } else {
      // Implicit receiver (e.g. inside a task configuration lambda)
      analyze(expression) {
        val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
        val receiverType = call.dispatchReceiver?.type ?: return@analyze
        if (receiverType.isSubtypeOf(Task)) {
          expression.report("Use a TaskProvider instead of a string in $calleeName()")
        }
      }
    }
  }

  private fun KtCallExpression.hasStringArgument(): Boolean = valueArguments.any {
    it.getArgumentExpression() is KtStringTemplateExpression
  }

  private companion object {
    val Task = FqName("org.gradle.api.Task")
    val WIRING_METHODS = setOf("dependsOn", "mustRunAfter", "shouldRunAfter", "finalizedBy")
  }
}
