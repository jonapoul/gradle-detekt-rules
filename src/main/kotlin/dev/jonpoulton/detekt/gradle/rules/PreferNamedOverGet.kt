package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Flags usages of `getByName`, `findByName`, and `getAt` (the `[]` operator) on
 * `NamedDomainObjectCollection` in favour of `named`.
 *
 * The eager lookup APIs (`getByName`, `findByName`, `getAt`) realise the container object
 * immediately at configuration time, preventing Gradle from skipping unnecessary work. `named`
 * returns a `NamedDomainObjectProvider` that defers realisation until the object is actually
 * needed, reducing configuration time and enabling task avoidance.
 *
 * Noncompliant:
 * ```kotlin
 * tasks.getByName("assemble") { dependsOn("myTask") }
 * val config = configurations.findByName("implementation")
 * val t = tasks["assemble"]
 * ```
 *
 * Compliant:
 * ```kotlin
 * tasks.named("assemble") { dependsOn("myTask") }
 * val config = configurations.named("implementation")
 * val t = tasks.named("assemble")
 * ```
 */
internal class PreferNamedOverGet(config: Config) :
  Rule(
    config = config,
    description =
      "named() should be preferred over getByName() and getAt() to avoid eager realisation of container " +
        "objects",
  ),
  RequiresAnalysisApi {
  @Configuration(
    "Allow findByName calls, which return null instead of throwing for missing elements"
  )
  private val allowFindByName: Boolean by config(defaultValue = false)

  private val activeMethods: Set<String>
    get() = if (allowFindByName) GET_METHODS - "findByName" else GET_METHODS

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    super.visitDotQualifiedExpression(expression)
    val selector = expression.selectorExpression ?: return
    val calleeName =
      when (selector) {
        is KtCallExpression -> selector.calleeExpression?.text
        is KtNameReferenceExpression -> selector.getReferencedName()
        else -> null
      } ?: return
    if (calleeName !in activeMethods) return

    analyze(expression) {
      val receiverType = expression.receiverExpression.expressionType ?: return@analyze
      if (receiverType.isSubtypeOf(NamedDomainObjectCollection)) {
        val receiverName = expression.receiverExpression.text
        expression.report("Prefer $receiverName.named over $receiverName.$calleeName")
      }
    }
  }

  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    super.visitArrayAccessExpression(expression)
    val array = expression.arrayExpression ?: return

    analyze(expression) {
      val arrayType = array.expressionType ?: return@analyze
      if (arrayType.isSubtypeOf(NamedDomainObjectCollection)) {
        val receiverName = array.text
        expression.report("Prefer $receiverName.named over $receiverName[...]")
      }
    }
  }

  private companion object {
    val NamedDomainObjectCollection = FqName("org.gradle.api.NamedDomainObjectCollection")
    val GET_METHODS = setOf("getByName", "findByName", "getAt")
  }
}
