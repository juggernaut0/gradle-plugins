
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
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
