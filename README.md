settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
    }
}

plugins {
    id("dev.twarner.settings") version "1.0.0"
}
```
