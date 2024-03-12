package dev.twarner.gradle

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolverStubsGeneratorTest {
    @Test
    fun `test generate`(@TempDir tempDir: Path) {
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

        val outputDir = tempDir.resolve("out")
        outputDir.createDirectories()

        generateResolverStubs(schemaFile, outputDir, "com.example")

        val generatedFile = outputDir.resolve("com/example/ExampleResolvers.kt").readText()
        val expected = """
            package com.example

            import graphql.GraphQLContext
            import graphql.schema.GraphQLSchema
            import graphql.schema.idl.RuntimeWiring
            import graphql.schema.idl.SchemaGenerator
            import graphql.schema.idl.SchemaParser
            
            public interface ExampleQueryResolver {
              public fun getGreeting(context: GraphQLContext, name: String?): ExampleGreetingResolver
            }
            
            public interface ExampleGreetingResolver {
              public fun getMessage(context: GraphQLContext): String
            
              public fun getRepeated(context: GraphQLContext, times: Int): List<String>
            }
            
            public fun createExampleSchema(resolver: ExampleQueryResolver): GraphQLSchema {
              val types = SchemaParser().parse(""${'"'}
                  |type Greeting {
                  |  message: String!
                  |  repeated(times: Int!): [String!]!
                  |}
                  |
                  |type Query {
                  |  greeting(name: String): Greeting!
                  |}
                  |""${'"'}.trimMargin())
              val wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query") { wiring -> 
                  wiring.dataFetcher("greeting") {
                    resolver.getGreeting(it.graphQlContext, it.getArgument("name"))
                  }
                }
                .type("Greeting") { wiring -> 
                  wiring.dataFetcher("message") {
                    it.getSource<ExampleGreetingResolver>().getMessage(it.graphQlContext)
                  }
                  wiring.dataFetcher("repeated") {
                    it.getSource<ExampleGreetingResolver>().getRepeated(it.graphQlContext,
                        it.getArgument("times"))
                  }
                }
                .build()
              return SchemaGenerator().makeExecutableSchema(types, wiring)
            }
            
        """.trimIndent()
        assertEquals(expected, generatedFile)
    }
}
