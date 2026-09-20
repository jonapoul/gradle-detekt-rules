package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class PreferNamedOverGetTest(private val env: KotlinEnvironmentContainer) {
  private val rule = PreferNamedOverGet(Config.empty)

  @Test
  fun `Don't report named`() {
    val code =
      """
      tasks.named("assemble")
      configurations.named("implementation")
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report getByName on non-container types`() {
    val code =
      """
      val map = mapOf("key" to "value")
      map.getOrDefault("key", "default")
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report array access on non-container types`() {
    val code =
      """
      val list = listOf("a", "b")
      val item = list[0]
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report getByName`() {
    val code =
      """
      tasks.getByName("assemble")
      configurations.getByName("implementation")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("Prefer tasks.named over tasks.getByName") }
      .onSecondFinding {
        messageContains("Prefer configurations.named over configurations.getByName")
      }
  }

  @Test
  fun `Report findByName`() {
    val code =
      """
      tasks.findByName("assemble")
      configurations.findByName("implementation")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("Prefer tasks.named over tasks.findByName") }
      .onSecondFinding {
        messageContains("Prefer configurations.named over configurations.findByName")
      }
  }

  @Test
  fun `Report array access operator`() {
    val code =
      """
      val t = tasks["assemble"]
      val c = configurations["implementation"]
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("Prefer tasks.named over tasks[...]") }
      .onSecondFinding { messageContains("Prefer configurations.named over configurations[...]") }
  }

  @Test
  fun `Don't report findByName when allowFindByName is true`() {
    val configuredRule = PreferNamedOverGet(TestConfig("allowFindByName" to true))
    val code =
      """
      tasks.findByName("assemble")
      configurations.findByName("implementation")
      """
        .trimIndent()

    assertThat(configuredRule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Still report getByName when allowFindByName is true`() {
    val configuredRule = PreferNamedOverGet(TestConfig("allowFindByName" to true))
    val code =
      """
      tasks.getByName("assemble")
      tasks.findByName("assemble")
      """
        .trimIndent()

    assertThat(configuredRule).lintedAsKts(env, code).hasNumFindings(expected = 1).onFirstFinding {
      messageContains("Prefer tasks.named over tasks.getByName")
    }
  }
}
