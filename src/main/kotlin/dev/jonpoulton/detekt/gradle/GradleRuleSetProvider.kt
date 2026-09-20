package dev.jonpoulton.detekt.gradle

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import dev.jonpoulton.detekt.gradle.rules.AvoidAfterEvaluate
import dev.jonpoulton.detekt.gradle.rules.AvoidCrossProjectConfiguration
import dev.jonpoulton.detekt.gradle.rules.AvoidEagerWithType
import dev.jonpoulton.detekt.gradle.rules.AvoidExtraProperties
import dev.jonpoulton.detekt.gradle.rules.AvoidProjectEquality
import dev.jonpoulton.detekt.gradle.rules.AvoidRootProjectAccess
import dev.jonpoulton.detekt.gradle.rules.AvoidStringTaskReferences
import dev.jonpoulton.detekt.gradle.rules.LazyCollectionOperators
import dev.jonpoulton.detekt.gradle.rules.PreferGradlePropertyProvider
import dev.jonpoulton.detekt.gradle.rules.PreferNamedOverGet
import dev.jonpoulton.detekt.gradle.rules.PreferRegisterOverCreate

public class GradleRuleSetProvider : RuleSetProvider {
  override val ruleSetId: RuleSetId = RuleSetId("gradle")

  override fun instance(): RuleSet =
    RuleSet(
      id = ruleSetId,
      rules =
        listOf(
          ::AvoidAfterEvaluate,
          ::AvoidEagerWithType,
          ::AvoidExtraProperties,
          ::AvoidCrossProjectConfiguration,
          ::AvoidProjectEquality,
          ::AvoidRootProjectAccess,
          ::AvoidStringTaskReferences,
          ::LazyCollectionOperators,
          ::PreferGradlePropertyProvider,
          ::PreferNamedOverGet,
          ::PreferRegisterOverCreate,
        ),
    )
}
