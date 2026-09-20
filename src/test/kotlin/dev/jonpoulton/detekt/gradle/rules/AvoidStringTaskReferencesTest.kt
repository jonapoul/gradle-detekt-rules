package dev.jonpoulton.detekt.gradle.rules

import assertk.assertThat
import dev.detekt.api.Config
import dev.detekt.test.junit.KotlinCoreEnvironmentTest
import dev.detekt.test.utils.KotlinEnvironmentContainer
import kotlin.test.Test

@KotlinCoreEnvironmentTest
internal class AvoidStringTaskReferencesTest(private val env: KotlinEnvironmentContainer) {
  private val rule = AvoidStringTaskReferences(Config.empty)

  @Test
  fun `Don't report TaskProvider references`() {
    val code =
      """
      val otherTask = tasks.register("otherTask")
      tasks.getByName("myTask").dependsOn(otherTask)
      tasks.getByName("myTask").mustRunAfter(otherTask)
      tasks.getByName("myTask").shouldRunAfter(otherTask)
      tasks.getByName("myTask").finalizedBy(otherTask)
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }

  @Test
  fun `Report string arguments with explicit receiver`() {
    val code =
      """
      tasks.getByName("myTask").dependsOn("otherTask")
      tasks.getByName("myTask").mustRunAfter("task2")
      tasks.getByName("myTask").shouldRunAfter("task3")
      tasks.getByName("myTask").finalizedBy("task4")
      """
        .trimIndent()

    assertThat(rule)
      .lintedAsKts(env, code)
      .hasNumFindings(expected = 4)
      .onFirstFinding { messageContains("Use a TaskProvider instead of a string in dependsOn()") }
      .onSecondFinding {
        messageContains("Use a TaskProvider instead of a string in mustRunAfter()")
      }
      .onThirdFinding {
        messageContains("Use a TaskProvider instead of a string in shouldRunAfter()")
      }
      .onFinding(number = 4) {
        messageContains("Use a TaskProvider instead of a string in finalizedBy()")
      }
  }

  @Test
  fun `Report string arguments with implicit receiver in getByName block`() {
    val code =
      """
      tasks.named("myTask").configure {
        it.dependsOn("otherTask")
      }
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNumFindings(expected = 1).onFirstFinding {
      messageContains("Use a TaskProvider instead of a string in dependsOn()")
    }
  }

  @Test
  fun `Don't report non-Task receiver`() {
    val code =
      """
      val list = listOf("a", "b")
      println(list.toString())
      """
        .trimIndent()

    assertThat(rule).lintedAsKts(env, code).hasNoFindings()
  }
}
