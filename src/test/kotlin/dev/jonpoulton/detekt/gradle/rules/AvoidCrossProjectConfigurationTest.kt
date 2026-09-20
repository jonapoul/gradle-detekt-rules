package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class AvoidCrossProjectConfigurationTest(private val env: KotlinEnvironmentContainer) {
  private val rule = AvoidCrossProjectConfiguration(Config.empty)

  @Test
  fun `Don't report unrelated calls`() {
    val code =
      """
      tasks.register("myTask")
      configurations.register("myConfig")
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report subprojects`() {
    val code =
      """
      subprojects { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("subprojects")
  }

  @Test
  fun `Report allprojects`() {
    val code =
      """
      allprojects { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("allprojects")
  }

  @Test
  fun `Report with explicit this receiver`() {
    val code =
      """
      this.subprojects { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("subprojects")
  }

  @Test
  fun `Report multiple cross-project calls`() {
    val code =
      """
      subprojects { }
      allprojects { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("subprojects") }
      .onSecondFinding { messageContains("allprojects") }
  }

  @Test
  fun `Don't report allprojects when allowAllprojects is true`() {
    val configuredRule = AvoidCrossProjectConfiguration(TestConfig("allowAllprojects" to true))
    val code =
      """
      allprojects { }
      """
        .trimIndent()

    assertThat(configuredRule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Still report subprojects when allowAllprojects is true`() {
    val configuredRule = AvoidCrossProjectConfiguration(TestConfig("allowAllprojects" to true))
    val code =
      """
      subprojects { }
      allprojects { }
      """
        .trimIndent()

    assertThat(configuredRule).lintedAsKts(env, code).hasNumFindings(expected = 1).onFirstFinding {
      messageContains("subprojects")
    }
  }
}
