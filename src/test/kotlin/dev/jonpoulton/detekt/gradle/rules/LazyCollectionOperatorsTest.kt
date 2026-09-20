package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class LazyCollectionOperatorsTest(private val env: KotlinEnvironmentContainer) {
  private val rule = LazyCollectionOperators(Config.empty)

  @Test
  fun `Don't report configureEach`() {
    val code =
      """
      tasks.configureEach { }
      configurations.configureEach { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report all on non-Gradle collection`() {
    val code =
      """
      val list = listOf(1, 2, 3)
      val result = list.all { it > 0 }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report all on tasks`() {
    val code =
      """
      tasks.all { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Prefer configureEach over all")
  }

  @Test
  fun `Report all on configurations`() {
    val code =
      """
      configurations.all { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Prefer configureEach over all")
  }

  @Test
  fun `Report whenObjectAdded`() {
    val code =
      """
      tasks.whenObjectAdded { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Prefer configureEach over whenObjectAdded")
  }

  @Test
  fun `Report whenTaskAdded`() {
    val code =
      """
      tasks.whenTaskAdded { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Prefer configureEach over whenTaskAdded")
  }

  @Test
  fun `Don't report forEach on non-Gradle collection`() {
    val code =
      """
      val list = listOf("a", "b")
      list.forEach { println(it) }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report forEach on tasks`() {
    val code =
      """
      tasks.forEach { println(it) }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Avoid calling forEach on Gradle collections")
  }

  @Test
  fun `Report filter on configurations`() {
    val code =
      """
      configurations.filter { it.name.startsWith("test") }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Avoid calling filter on Gradle collections")
  }

  @Test
  fun `Report map on tasks`() {
    val code =
      """
      tasks.map { it.name }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Avoid calling map on Gradle collections")
  }

  @Test
  fun `Report first on tasks`() {
    val code =
      """
      tasks.first()
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Avoid calling first on Gradle collections")
  }

  @Test
  fun `Report toList on tasks`() {
    val code =
      """
      tasks.toList()
      tasks.toSet()
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasTwoFindings()
      .onFirstFinding { messageContains("Avoid calling toList on Gradle collections") }
      .onSecondFinding { messageContains("Avoid calling toSet on Gradle collections") }
  }

  @Test
  fun `Report multiple eager operators`() {
    val code =
      """
      tasks.all { }
      configurations.all { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 2)
      .onFirstFinding { messageContains("configureEach") }
      .onSecondFinding { messageContains("configureEach") }
  }

  @Test
  fun `tasks withType doesn't report`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task

      val tasks1 = tasks.withType(MyTask::class.java)
      val tasks2 = tasks.withType<MyTask>()
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }
}
