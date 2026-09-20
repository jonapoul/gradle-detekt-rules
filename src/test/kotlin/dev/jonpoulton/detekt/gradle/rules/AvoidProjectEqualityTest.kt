package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class AvoidProjectEqualityTest(private val env: KotlinEnvironmentContainer) {
  private val rule = AvoidProjectEquality(Config.empty)

  @Test
  fun `Don't report equality on non-Project types`() {
    val code =
      """
      val a = "foo"
      val b = "bar"
      val eq = a == b
      val neq = a != b
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report comparing project paths`() {
    val code =
      """
      val other: Project = TODO()
      val same = path == other.path
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report == on Project`() {
    val code =
      """
      val other: Project = TODO()
      val eq = this == other
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("project.path")
  }

  @Test
  fun `Report != on Project`() {
    val code =
      """
      val other: Project = TODO()
      val neq = this != other
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("project.path")
  }

  @Test
  fun `Report === on Project`() {
    val code =
      """
      val other: Project = TODO()
      val eq = this === other
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("project.path")
  }

  @Test
  fun `Report !== on Project`() {
    val code =
      """
      val other: Project = TODO()
      val neq = this !== other
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("project.path")
  }

  @Test
  fun `Report explicit equals call on Project`() {
    val code =
      """
      val other: Project = TODO()
      this.equals(other)
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().messageContains("project.path")
  }

  @Test
  fun `Report multiple equality checks`() {
    val code =
      """
      val a: Project = TODO()
      val b: Project = TODO()
      val eq = a == b
      val neq = a != b
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("project.path") }
      .onSecondFinding { messageContains("project.path") }
  }
}
