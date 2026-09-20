package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class AvoidExtraPropertiesTest(private val env: KotlinEnvironmentContainer) {
  private val rule = AvoidExtraProperties(Config.empty)

  @Test
  fun `Don't report unrelated properties`() {
    val code =
      """
      val n = name
      val p = path
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report local variable named extra`() {
    val code =
      """
      val extra = mapOf("key" to "value")
      println(extra)
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report extra access with implicit receiver`() {
    val code =
      """
      val e = extra
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("Extra")
  }

  @Test
  fun `Report extra access with explicit this`() {
    val code =
      """
      val e = this.extra
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("Extra")
  }

  @Test
  fun `Report extra access with Project receiver`() {
    val code =
      """
      val p: Project = TODO()
      val e = p.extra
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("Extra")
  }

  @Test
  fun `Report multiple extra accesses`() {
    val code =
      """
      val a = extra
      val b = extra
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("Extra") }
      .onSecondFinding { messageContains("Extra") }
  }
}
