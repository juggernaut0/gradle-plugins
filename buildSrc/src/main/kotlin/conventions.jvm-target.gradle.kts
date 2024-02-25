import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType<JavaCompile>().configureEach {
        options.release = 17
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }
}
