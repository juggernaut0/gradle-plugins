# GraphQL plugin

Generates resolver stubs and typesafe query object from GraphQL schema and query files.

## Usage

```kotlin
plugins {
    kotlin("...") // i.e. jvm, multiplatform
    id("dev.twarner.kotlin-graphql")
}

dependencies {
    graphqlValidationSchema(project(":project-with-schema"))
}
```

Put schema and queries in `src/main/graphql`. Schema files are named *.graphqls. Query files are named *.graphqlq.

The plugin will generate Kotlin sources for resolver stubs for any schema files present. It will also validate queries 
and generate query objects for any query files present.

Kotlin JVM plugin must be applied if schema files are present.

## Schemas

See: https://graphql.org/learn/schema/

There will be an interface generated per type in the schema, including the top-level Query type. This interface is 
expected to be implemented by the user, and the implementation registered with the execution engine.

`example.graphqls`

```
type Query {
    greeting(a: String!): String!
}
```

```kotlin
// generated
interface ExampleQueryResolver {
    fun getGreeting(context: QueryContext, a: String): String
}

fun RuntimeWiring.registerResolvers(exampleQueryResolver: ExampleQueryResolver) {
    // ...
}

// usage
class ExampleQueryImpl : ExampleQueryResolver {
    override fun getGreeting(context: QueryContext, a: String): String {
        return "hello $a"
    }
}

fun main() {
    val runtimeWiring = newRuntimeWiring()
        .registerResolvers(ExampleQueryImpl())
        .build()
}
```

Schemas are exported as outgoing artifacts for other projects to consume for validation, and can be published to a maven 
repository with the maven-publish plugin.

## Queries

At build time, query files will be validated against the schema specified in the `# schema` comment on the first line of 
the query file. If this comment is not present, the plugin will try to validate against *any* schema that is present or 
has been imported. If the query cannot be validated, the build will fail.

There will be at least two class generated per query file, a query and a query response. The query will take any query 
variables as constructor parameters, and the response will contain properties corresponding to the queried fields.

`example.graphqlq`

```
# schema example
query($v:String!){ greeting(a: $v) }
```

```kotlin
// generated
class ExampleQuery(val v: String): Query {
    override fun toRequest(): GraphqlRequest { 
        // ...
    }
}

data class ExampleQueryResponse(val greeting: String)

// usage
fun main() {
    val resp = graphqlClient.call(ExampleQuery("world"))
    println(resp.greeting)
}
```
