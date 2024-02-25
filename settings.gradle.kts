
rootProject.name = "gradle-plugins"

include(
    "catalog",
    "catalog:test",
    "common-utils",
    "db",
    "docker",
    "download-firefox",
    "kotlin-api",
    "kotlin-service",
    "kotlin-web",
    "kotlin-yarn",
    "sass",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
