package dev.jonpoulton.detekt.gradle.rules

import com.intellij.psi.PsiElement
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.FqName

context(rule: Rule)
internal fun PsiElement.report(message: String) {
  rule.report(Finding(entity = Entity.from(this), message = message))
}

context(session: KaSession)
internal fun KaType.isSubtypeOf(fqName: FqName): Boolean =
  with(session) {
    allSupertypes.plus(this@isSubtypeOf).any { type ->
      type.expandedSymbol?.classId?.asSingleFqName() == fqName
    }
  }

internal val GRADLE_PROJECT = FqName("org.gradle.api.Project")
