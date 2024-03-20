package dev.twarner.gradle

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryGeneratorTest {
    @Test
    fun foo(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("example.graphqls")
        schemaFile.writeText(
            """
                type Query {
                    greeting(name: String): Greeting!
                }
                
                type Greeting {
                    message: String!
                    repeated(times: Int!): [String!]!
                }
            """.trimIndent()
        )
        val queryFile = tempDir.resolve("query.graphql")
        queryFile.writeText(
            """
                query(${'$'}name: String) {
                    greeting(name: ${'$'}name) {
                        message
                        repeated(times: 3)
                    }
                }
            """.trimIndent()
        )
        val outDir = tempDir.resolve("out")

        generateQuery(queryFile, schemaFile, outDir, "com.example")

        val generatedFile = outDir.resolve("com/example/QueryQuery.kt").readText()
        val expected = """
            package com.example
            
            import kotlinx.serialization.DeserializationStrategy
            import kotlinx.serialization.Serializable
            import multiplatform.graphql.GraphQLQuery
            
            public class QueryQuery(
              name: String?,
            ) : GraphQLQuery<QueryQuery.Response> {
              override val queryString: String = ""${'"'}
                  |query(${"\${'$'}"}name: String) {
                  |  greeting(name: ${"\${'$'}"}name) {
                  |    message
                  |    repeated(times: 3)
                  |  }
                  |}
                  |""${'"'}.trimMargin()
            
              override val variables: Map<String, Any?> = mapOf(
                  "name" to name,
                  )
            
              override val responseDeserializer: DeserializationStrategy<Response> = Response.serializer()
            
              @Serializable
              public data class Response(
                public val greeting: Greeting,
              ) {
                @Serializable
                public data class Greeting(
                  public val message: String,
                  public val repeated: List<String>,
                )
              }
            }
            
        """.trimIndent()
        assertEquals(expected, generatedFile)
    }
}