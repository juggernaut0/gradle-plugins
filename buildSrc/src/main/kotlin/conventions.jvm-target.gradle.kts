import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget = JvmTarget.JVM_21
    }
}
