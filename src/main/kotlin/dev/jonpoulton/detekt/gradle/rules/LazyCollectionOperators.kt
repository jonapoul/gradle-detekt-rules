package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import dev.jonpoulton.detekt.gradle.rules.LazyCollectionOperators.Companion.EAGER_GRADLE_METHODS
import dev.jonpoulton.detekt.gradle.rules.LazyCollectionOperators.Companion.EAGER_STDLIB_OPERATORS
import dev.jonpoulton.detekt.gradle.rules.LazyCollectionOperators.Companion.LAZY_METHODS
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags eager collection operators on Gradle's `DomainObjectCollection` types in favour of their
 * lazy equivalents.
 *
 * Eager operators force realisation of container elements at configuration time, defeating task
 * avoidance and increasing build times. This rule catches both Gradle-specific eager APIs (`all`,
 * `whenObjectAdded`, `whenTaskAdded`) and Kotlin stdlib collection functions (`forEach`, `filter`,
 * `map`, `first`, etc.) when called on Gradle collections.
 *
 * Every method name the rule cares about is listed explicitly, in [LAZY_METHODS],
 * [EAGER_GRADLE_METHODS] and [EAGER_STDLIB_OPERATORS]. Anything not named there is left alone. This
 * rule only covers configuring and iterating a collection: creating elements is handled by
 * [PreferRegisterOverCreate], and looking them up by [PreferNamedOverGet].
 *
 * Noncompliant:
 * ```kotlin
 * tasks.all { enabled = true }
 * val testTasks = tasks.filter { it.name.startsWith("test") }
 * val myTasks = tasks.filterIsInstance<MyTask>()
 * ```
 *
 * Compliant:
 * ```kotlin
 * tasks.configureEach { enabled = true }
 * val testTasks = tasks.matching { it.name.startsWith("test") }
 * val myTasks = tasks.withType<MyTask>()
 * ```
 */
internal class LazyCollectionOperators(config: Config) :
  Rule(
    config = config,
    description =
      "When working with Gradle collection objects, prefer lazy operators over eager ones",
  ),
  RequiresAnalysisApi {
  @Configuration("Additional eager method names to flag on DomainObjectCollection types")
  private val additionalEagerMethods: List<String> by config(defaultValue = emptyList())

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    val calleeName = expression.calleeExpression?.text ?: return
    if (calleeName in LAZY_METHODS) return

    analyze(expression) {
      val call = expression.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
      reportEagerGradleMethod(expression, calleeName, call) ||
        reportEagerStdlibOperator(expression, calleeName, call)
    }
  }

  /**
   * Eager members of the Gradle collection itself, such as `tasks.all { }`. These are called on a
   * dispatch receiver, and each has a targeted lazy replacement to suggest.
   */
  context(_: KaSession)
  private fun reportEagerGradleMethod(
    expression: KtCallExpression,
    calleeName: String,
    call: KaFunctionCall<*>,
  ): Boolean {
    val replacement = EAGER_GRADLE_METHODS[calleeName]
    val isEager = replacement != null || calleeName in additionalEagerMethods
    val dispatchType = call.dispatchReceiver?.type
    if (!isEager || dispatchType?.isSubtypeOf(DomainObjectCollection) != true) return false

    expression.report(
      if (replacement != null) {
        "Prefer $replacement over $calleeName for lazy configuration"
      } else {
        "Avoid calling eager method $calleeName on Gradle collections"
      }
    )
    return true
  }

  context(_: KaSession)
  private fun reportEagerStdlibOperator(
    expression: KtCallExpression,
    calleeName: String,
    call: KaFunctionCall<*>,
  ): Boolean {
    val isEagerStdlibOperator =
      calleeName in EAGER_STDLIB_OPERATORS &&
        call.symbol.callableId?.packageName == KotlinCollections
    if (!isEagerStdlibOperator || !call.extensionReceiver?.type.isIterableGradleCollection()) {
      return false
    }

    expression.report(
      "Avoid calling $calleeName on Gradle collections - it forces eager realization of all elements"
    )
    return true
  }

  context(_: KaSession)
  private fun KaType?.isIterableGradleCollection(): Boolean =
    this != null && isSubtypeOf(DomainObjectCollection) && isSubtypeOf(KotlinIterable)

  private companion object {
    val DomainObjectCollection = FqName("org.gradle.api.DomainObjectCollection")
    val KotlinIterable = FqName("kotlin.collections.Iterable")
    val KotlinCollections = FqName("kotlin.collections")

    const val CONFIGURE_EACH = "configureEach"

    val LAZY_METHODS = setOf(CONFIGURE_EACH, "matching", "withType")

    val EAGER_GRADLE_METHODS =
      mapOf(
        "all" to CONFIGURE_EACH,
        "whenObjectAdded" to CONFIGURE_EACH,
        "whenTaskAdded" to CONFIGURE_EACH,
      )

    val EAGER_STDLIB_OPERATORS =
      setOf(
        "any",
        "asReversed",
        "associate",
        "associateBy",
        "associateWith",
        "count",
        "distinct",
        "distinctBy",
        "drop",
        "dropWhile",
        "filter",
        "filterIndexed",
        "filterIsInstance",
        "filterNot",
        "filterNotNull",
        "find",
        "findLast",
        "first",
        "firstNotNullOf",
        "firstNotNullOfOrNull",
        "firstOrNull",
        "flatMap",
        "fold",
        "forEach",
        "forEachIndexed",
        "groupBy",
        "indexOfFirst",
        "indexOfLast",
        "joinToString",
        "last",
        "lastOrNull",
        "map",
        "mapIndexed",
        "mapNotNull",
        "maxByOrNull",
        "maxOf",
        "maxOfOrNull",
        "minByOrNull",
        "minOf",
        "minOfOrNull",
        "none",
        "onEach",
        "partition",
        "reduce",
        "reversed",
        "single",
        "singleOrNull",
        "sortedBy",
        "sortedByDescending",
        "sortedWith",
        "sumOf",
        "take",
        "takeWhile",
        "toList",
        "toMutableList",
        "toMutableSet",
        "toSet",
        "toTypedArray",
      )
  }
}
