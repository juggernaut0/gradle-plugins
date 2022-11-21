plugins {
    `version-catalog`
    `maven-publish`
}

group = "dev.twarner"
val projectVersion: String by rootProject
version = projectVersion

catalog {
    versionCatalog {
        this.from(files(projectDir.resolve("catalog.versions.toml")))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }
    repositories {
        maven {
            name = "pages"
            url = uri("$rootDir/pages/m2/repository")
        }
    }
}
