settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
    }

    plugins {
        id("dev.twarner.common") version "0.2.1"

        val kotlinVersion = "1.7.21"
        kotlin("js") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://juggernaut0.github.io/m2/repository")
    }

    versionCatalogs {
        create("libs") {
            from("dev.twarner:catalog:0.2.1")
        }
    }
}
```

root build.gradle.kts

```kotlin
plugins {
    id("dev.twarner.common") apply false
}
```