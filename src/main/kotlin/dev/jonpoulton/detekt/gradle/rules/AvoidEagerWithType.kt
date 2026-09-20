package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags `withType` calls that take a configuration action, in favour of `withType(...)` followed by
 * `configureEach`.
 *
 * `withType(type) { }` is a shorthand for `withType(type).all { }`, so it realises and configures
 * every matching element as soon as the build script is evaluated. Chaining `configureEach` onto
 * the filtered collection defers that configuration until each element is actually needed, keeping
 * task avoidance and the configuration cache effective.
 *
 * All three ways of naming the type are covered - the Java class, the Kotlin class, and the reified
 * type parameter, including when the reified type is left to be inferred.
 *
 * Noncompliant:
 * ```kotlin
 * tasks.withType(MyTask::class.java) { enabled = true }
 * tasks.withType(MyTask::class) { enabled = true }
 * tasks.withType<MyTask> { enabled = true }
 * ```
 *
 * Compliant:
 * ```kotlin
 * tasks.withType(MyTask::class.java).configureEach { enabled = true }
 * tasks.withType(MyTask::class).configureEach { enabled = true }
 * tasks.withType<MyTask>().configureEach { enabled = true }
 * ```
 */
internal class AvoidEagerWithType(config: Config) :
  Rule(
    config = config,
    description = "withType() should filter lazily - configure the result with configureEach",
  ),
  RequiresAnalysisApi {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    if (expression.calleeExpression?.text != WITH_TYPE) return

    analyze(expression) {
      val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
      val receiverType = call.dispatchReceiver?.type ?: call.extensionReceiver?.type
      if (receiverType?.isSubtypeOf(DomainObjectCollection) != true) return@analyze

      val hasActionParameter =
        call.symbol.valueParameters.any { parameter -> parameter.returnType.isActionType() }
      if (!hasActionParameter) return@analyze

      expression.report("Prefer withType(...).configureEach { } over withType(...) { }")
    }
  }

  /**
   * Gradle's `Closure` overload comes through as a raw Java type, which has no symbol of its own to
   * compare against, so unwrap it to its lower bound first.
   */
  context(_: KaSession)
  private fun KaType.isActionType(): Boolean {
    val type = (this as? KaFlexibleType)?.lowerBound ?: this
    return ACTION_TYPES.any { actionType -> type.isSubtypeOf(actionType) }
  }

  private companion object {
    const val WITH_TYPE = "withType"

    val DomainObjectCollection = FqName("org.gradle.api.DomainObjectCollection")

    /** Every shape a `withType` configuration argument can take - Gradle, Kotlin and Groovy. */
    val ACTION_TYPES =
      listOf(
        FqName("groovy.lang.Closure"),
        FqName("kotlin.Function"),
        FqName("org.gradle.api.Action"),
      )
  }
}
