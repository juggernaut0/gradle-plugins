
rootProject.name = "gradle-plugins"

include(
    "catalog",
    "catalog:test",
    "common-utils",
    "db",
    "docker",
    "download-firefox",
    "kotlin",
    "kotlin-api",
    "kotlin-service",
    "kotlin-web",
    "sass",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
