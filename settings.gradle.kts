
rootProject.name = "gradle-plugins"

include(
    "catalog",
    "catalog:test",
    "db",
    "download-firefox",
    "kotlin",
    "kotlin-api",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
