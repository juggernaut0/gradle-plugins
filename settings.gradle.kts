
rootProject.name = "gradle-plugins"

include(
    "catalog",
    "catalog:test",
    "download-firefox",
    "kotlin",
    "kotlin-api",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
