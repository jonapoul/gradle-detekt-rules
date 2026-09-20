package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Flags usages of `create`, `maybeCreate`, and `creating` on `NamedDomainObjectContainer` in favour
 * of their lazy equivalents `register` and `registering`.
 *
 * Eager creation APIs (`create`, `maybeCreate`, `creating`) realise the container object
 * immediately, forcing configuration to run at the time the build script is evaluated. This
 * increases configuration time and can prevent Gradle from skipping unnecessary work. The lazy APIs
 * (`register`, `registering`) defer object creation and configuration until the object is actually
 * needed, which is required for configuration cache compatibility and task avoidance.
 *
 * Noncompliant:
 * ```kotlin
 * tasks.create("myTask") { doLast { println("hello") } }
 * val myConfig by configurations.creating
 * ```
 *
 * Compliant:
 * ```kotlin
 * tasks.register("myTask") { doLast { println("hello") } }
 * val myConfig by configurations.registering
 * ```
 */
internal class PreferRegisterOverCreate(config: Config) :
  Rule(
    config,
    "Gradle objects should be created lazily where possible - prefer register() over create()",
  ),
  RequiresAnalysisApi {
  @Configuration("Allow maybeCreate calls, which are idempotent and sometimes necessary in plugins")
  private val allowMaybeCreate: Boolean by config(defaultValue = false)

  private val activeMethods: Set<String>
    get() = if (allowMaybeCreate) CREATE_METHODS - "maybeCreate" else CREATE_METHODS

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
    expression.checkAndReport(expression.receiverExpression, calleeName)
  }

  private fun KtExpression.checkAndReport(receiver: KtExpression, calleeName: String) {
    analyze(this) {
      val receiverType = receiver.expressionType ?: return@analyze

      if (receiverType.isSubtypeOf(NamedDomainObjectContainer)) {
        val receiverName = receiver.text
        report("Prefer $receiverName.register over $receiverName.$calleeName")
      }
    }
  }

  private companion object {
    val NamedDomainObjectContainer = FqName("org.gradle.api.NamedDomainObjectContainer")
    val CREATE_METHODS = setOf("add", "addAll", "create", "creating", "maybeCreate")
  }
}
