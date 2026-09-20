package dev.jonpoulton.detekt.gradle.rules

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Flags accesses to `Project.rootProject` that break project isolation.
 *
 * Accessing `rootProject` directly couples a subproject to the root project's configuration,
 * breaking Gradle's project isolation model and preventing use of isolated projects. Use
 * `rootProject.isolated` for safe, limited access to root project properties (like `rootDir` or
 * `layout`), or prefer shared build services and convention plugins to share configuration.
 *
 * Noncompliant:
 * ```kotlin
 * val rootName = rootProject.name
 * val rootDir = rootProject.layout.projectDirectory
 * ```
 *
 * Compliant:
 * ```kotlin
 * val rootDir = rootProject.isolated.rootDirectory
 * ```
 */
internal class AvoidRootProjectAccess(config: Config) :
  Rule(
    config = config,
    description =
      "rootProject access breaks project isolation - use rootProject.isolated, services, or convention plugins " +
        "instead",
  ),
  RequiresAnalysisApi {
  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    if (expression.getReferencedName() != ROOT_PROJECT) return
    if (expression.isFollowedByIsolated()) return

    analyze(expression) {
      val receiverType =
        expression.resolveToCall()?.singleCallOrNull<KaVariableAccessCall>()?.dispatchReceiver?.type
          ?: return@analyze
      if (receiverType.isSubtypeOf(GRADLE_PROJECT)) {
        expression.report(description)
      }
    }
  }

  @Suppress("ReturnCount")
  private fun KtSimpleNameExpression.isFollowedByIsolated(): Boolean {
    val parent = parent as? KtDotQualifiedExpression ?: return false

    // rootProject.isolated
    if (parent.receiverExpression == this) {
      return (parent.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ==
        ISOLATED
    }

    // this.rootProject.isolated or project.rootProject.isolated
    if (parent.selectorExpression == this) {
      val grandparent = parent.parent as? KtDotQualifiedExpression ?: return false
      if (grandparent.receiverExpression == parent) {
        return (grandparent.selectorExpression as? KtNameReferenceExpression)
          ?.getReferencedName() == ISOLATED
      }
    }

    return false
  }

  private companion object {
    const val ROOT_PROJECT = "rootProject"
    const val ISOLATED = "isolated"
  }
}
