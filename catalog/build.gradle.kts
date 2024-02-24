plugins {
    `version-catalog`
    `maven-publish`
}

group = "dev.twarner"

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
}
