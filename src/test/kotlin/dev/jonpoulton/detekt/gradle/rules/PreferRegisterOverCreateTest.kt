package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class PreferRegisterOverCreateTest(private val env: KotlinEnvironmentContainer) {
  private val rule = PreferRegisterOverCreate(Config.empty)

  @Test
  fun `Don't report register`() {
    val code =
      """
      configurations.register("config1")
      configurations.register("config1") { }
      val config2 by configurations.registering
      val config3 by configurations.registering { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report create`() {
    val code =
      """
      configurations.create("config1")
      configurations.create("config2") { }
      configurations.maybeCreate("config3")
      val config4 by configurations.creating
      val config5 by configurations.creating { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 5)
      .onFirstFinding {
        messageContains("Prefer configurations.register over configurations.create")
      }
      .onSecondFinding {
        messageContains("Prefer configurations.register over configurations.create")
      }
      .onThirdFinding {
        messageContains("Prefer configurations.register over configurations.maybeCreate")
      }
      .onFinding(number = 4) {
        messageContains("Prefer configurations.register over configurations.creating")
      }
      .onFinding(number = 5) {
        messageContains("Prefer configurations.register over configurations.creating")
      }
  }

  @Test
  fun `Don't report maybeCreate when allowMaybeCreate is true`() {
    val configuredRule = PreferRegisterOverCreate(TestConfig("allowMaybeCreate" to true))
    val code =
      """
      configurations.maybeCreate("config1")
      """
        .trimIndent()

    assertThat(configuredRule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Still report create and creating when allowMaybeCreate is true`() {
    val configuredRule = PreferRegisterOverCreate(TestConfig("allowMaybeCreate" to true))
    val code =
      """
      configurations.create("config1")
      configurations.maybeCreate("config2")
      val config3 by configurations.creating
      """
        .trimIndent()

    assertThat(configuredRule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding {
        messageContains("Prefer configurations.register over configurations.create")
      }
      .onSecondFinding {
        messageContains("Prefer configurations.register over configurations.creating")
      }
  }
}
