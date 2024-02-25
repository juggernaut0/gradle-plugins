plugins {
    `version-catalog`
    `maven-publish`
}

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
