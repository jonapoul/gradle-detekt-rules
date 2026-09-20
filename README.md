# Gradle Detekt Rules

[![Latest release](https://img.shields.io/github/v/release/jonapoul/gradle-detekt-rules)](https://github.com/jonapoul/gradle-detekt-rules/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jonpoulton.detekt/gradle-detekt-rules)](https://central.sonatype.com/artifact/dev.jonpoulton.detekt/gradle-detekt-rules)

A [detekt](https://detekt.dev) ruleset for Gradle build scripts and plugins, focusing on
configuration-cache safety, lazy APIs and project isolation.

## Usage

```kotlin
plugins {
  id("dev.detekt")
}

dependencies {
  detektPlugins("dev.jonpoulton.detekt:gradle-detekt-rules:<version>")
}
```

## Rules

| Rule | Description |
| --- | --- |
| `AvoidAfterEvaluate` | `afterEvaluate` breaks project isolation and is incompatible with configuration cache |
| `AvoidCrossProjectConfiguration` | `subprojects`/`allprojects` break project isolation - use convention plugins instead |
| `AvoidEagerWithType` | `withType()` should filter lazily - configure the result with `configureEach` |
| `AvoidExtraProperties` | Extra properties are eager and untyped - prefer typed extensions or convention plugins |
| `AvoidProjectEquality` | Comparing `Project` instances with `==` or `equals()` is unreliable - compare `project.path` instead |
| `AvoidRootProjectAccess` | `rootProject` access breaks project isolation - use `rootProject.isolated`, services, or convention plugins instead |
| `AvoidStringTaskReferences` | Task wiring methods should reference `TaskProvider`s rather than string task names |
| `LazyCollectionOperators` | When working with Gradle collection objects, prefer lazy operators over eager ones |
| `PreferGradlePropertyProvider` | Prefer `providers.gradleProperty` over direct property access for configuration cache compatibility |
| `PreferNamedOverGet` | `named()` should be preferred over `getByName()` and `getAt()` to avoid eager realisation |
| `PreferRegisterOverCreate` | Gradle objects should be created lazily where possible - prefer `register()` over `create()` |

See [config.yml](src/main/resources/config/config.yml) for the default configuration.
