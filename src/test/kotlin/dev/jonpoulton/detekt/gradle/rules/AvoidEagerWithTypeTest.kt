package dev.jonpoulton.detekt.gradle.rules

import assertk.Assert
import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test
import org.intellij.lang.annotations.Language

@KotlinCoreEnvironmentTest
internal class AvoidEagerWithTypeTest(private val env: KotlinEnvironmentContainer) {
  private val rule = AvoidEagerWithType(Config.empty)

  /**
   * Gradle's Kotlin DSL has no `KClass` overload of `withType`, so builds that want one declare it
   * themselves. Declared here too, to check the rule covers it.
   */
  @Language("kotlin")
  private val kClassWithType =
    """
    fun <T : Any, S : T> DomainObjectCollection<T>.withType(
      type: kotlin.reflect.KClass<S>,
    ): DomainObjectCollection<S> = TODO()

    fun <T : Any, S : T> DomainObjectCollection<T>.withType(
      type: kotlin.reflect.KClass<S>,
      configuration: S.() -> Unit,
    ): DomainObjectCollection<S> = TODO()
    """
      .trimIndent()

  @Test
  fun `Don't report withType without an action`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task
      $kClassWithType

      val tasks1 = tasks.withType(MyTask::class.java)
      val tasks2 = tasks.withType(MyTask::class)
      val tasks3 = tasks.withType<MyTask>()
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Don't report withType followed by configureEach`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task
      $kClassWithType

      tasks.withType(MyTask::class.java).configureEach { }
      tasks.withType(MyTask::class).configureEach { }
      tasks.withType<MyTask>().configureEach { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report withType with an action`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task
      $kClassWithType

      tasks.withType(MyTask::class.java) { }
      tasks.withType(MyTask::class) { }
      tasks.withType<MyTask> { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 3)
      .onFirstFinding { containsExpectedMessage() }
      .onSecondFinding { containsExpectedMessage() }
      .onThirdFinding { containsExpectedMessage() }
  }

  @Test
  fun `Report withType with an inferred type`() {
    val code =
      """
      tasks.withType { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().containsExpectedMessage()
  }

  @Test
  fun `Report withType with an action passed as an argument`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task

      val action = Action<MyTask> { }
      tasks.withType(MyTask::class.java, action)
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasOneFinding().containsExpectedMessage()
  }

  @Test
  fun `Report withType with a Groovy closure`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task

      tasks.withType(MyTask::class.java, closureOf<MyTask> { })
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasOneFinding()
      .messageContains("Prefer withType(...).configureEach { }")
  }

  @Test
  fun `Report withType however the collection is reached`() {
    val code =
      """
      abstract class MyTask : org.gradle.api.Task

      project.tasks.withType(MyTask::class.java) { }
      with(tasks) { withType<MyTask> { } }
      tasks { withType<MyTask> { } }
      val maybe: TaskCollection<Task>? = null
      maybe?.withType<MyTask> { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 4)
      .onFirstFinding { messageContains("Prefer withType(...).configureEach { }") }
      .onSecondFinding { messageContains("Prefer withType(...).configureEach { }") }
      .onThirdFinding { messageContains("Prefer withType(...).configureEach { }") }
      .onFinding(number = 4) { messageContains("Prefer withType(...).configureEach { }") }
  }

  @Test
  fun `Report withType on other Gradle collections`() {
    val code =
      """
      configurations.withType(Configuration::class.java) { }
      configurations.withType<Configuration> { }
      plugins.withType<Plugin<*>> { }
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 3)
      .onFirstFinding { containsExpectedMessage() }
      .onSecondFinding { containsExpectedMessage() }
      .onThirdFinding { containsExpectedMessage() }
  }

  private fun Assert<Finding>.containsExpectedMessage() =
    messageContains("Prefer withType(...).configureEach { }")

  @Test
  fun `Don't report withType on non-Gradle types`() {
    val code =
      """
      class NotAContainer {
        fun withType(type: Class<*>, action: () -> Unit) = Unit
      }

      NotAContainer().withType(String::class.java) { }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }
}
