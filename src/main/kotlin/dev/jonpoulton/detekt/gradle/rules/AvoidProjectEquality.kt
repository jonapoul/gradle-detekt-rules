package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Flags equality checks (`==`, `!=`, `===`, `!==`, `.equals()`) on `Project` instances.
 *
 * Since Gradle 9, referential equality is no longer guaranteed for `Project` instances. Different
 * references to the same logical project may point to distinct objects, so both structural (`==`)
 * and referential (`===`) equality can produce incorrect results. Compare `Project.path` values
 * instead.
 *
 * See
 * https://docs.gradle.org/current/userguide/upgrading_version_9.html#referential_equality_is_not_guaranteed_for_project_instances
 *
 * Noncompliant:
 * ```kotlin
 * val same = project == otherProject
 * val different = project != otherProject
 * project.equals(otherProject)
 * ```
 *
 * Compliant:
 * ```kotlin
 * val same = project.path == otherProject.path
 * val different = project.path != otherProject.path
 * ```
 */
internal class AvoidProjectEquality(config: Config) :
  Rule(
    config = config,
    description =
      "Comparing Project instances with == or equals() is unreliable - compare project.path instead",
  ),
  RequiresAnalysisApi {
  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    super.visitBinaryExpression(expression)
    if (expression.operationToken !in EQUALITY_OPERATORS) return

    analyze(expression) {
      val leftType = expression.left?.expressionType ?: return@analyze
      val rightType = expression.right?.expressionType ?: return@analyze
      if (leftType.isSubtypeOf(GRADLE_PROJECT) || rightType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.operationReference.report(description)
      }
    }
  }

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    super.visitDotQualifiedExpression(expression)
    val selector = expression.selectorExpression as? KtCallExpression ?: return
    if (selector.calleeExpression?.text != EQUALS) return

    analyze(expression) {
      val receiverType = expression.receiverExpression.expressionType ?: return@analyze
      if (receiverType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.report(description)
      }
    }
  }

  private companion object {
    const val EQUALS = "equals"
    val EQUALITY_OPERATORS =
      setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)
  }
}
