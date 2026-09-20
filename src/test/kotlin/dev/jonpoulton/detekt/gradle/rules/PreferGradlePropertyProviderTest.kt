package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class PreferGradlePropertyProviderTest(private val env: KotlinEnvironmentContainer) {
  private val rule = PreferGradlePropertyProvider(Config.empty)

  @Test
  fun `Don't report providers gradleProperty`() {
    val code =
      """
      val value = providers.gradleProperty("myProp")
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report property on non-Project types`() {
    val code =
      """
      val props = java.util.Properties()
      props.getProperty("key")
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report property`() {
    val code =
      """
      val value = property("myProp")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("providers.gradleProperty")
  }

  @Test
  fun `Report findProperty`() {
    val code =
      """
      val value = findProperty("myProp")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("providers.gradleProperty")
  }

  @Test
  fun `Report hasProperty`() {
    val code =
      """
      val exists = hasProperty("myProp")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("providers.gradleProperty")
  }

  @Test
  fun `Report with explicit this receiver`() {
    val code =
      """
      val value = this.property("myProp")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("providers.gradleProperty")
  }

  @Test
  fun `Report multiple property accesses`() {
    val code =
      """
      val a = property("prop1")
      val b = findProperty("prop2")
      val c = hasProperty("prop3")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 3)
      .onFirstFinding { messageContains("property") }
      .onSecondFinding { messageContains("findProperty") }
      .onThirdFinding { messageContains("hasProperty") }
  }
}
