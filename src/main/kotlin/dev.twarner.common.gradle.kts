import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

val projectVersion: String by rootProject
version = projectVersion

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://juggernaut0.github.io/m2/repository")
    google()
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinTopLevelExtension>("kotlin") {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
