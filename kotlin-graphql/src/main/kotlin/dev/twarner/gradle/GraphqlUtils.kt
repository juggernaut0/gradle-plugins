package dev.twarner.gradle

import graphql.language.InterfaceTypeDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.nio.file.Path
import kotlin.io.path.reader

fun unwiredSchema(path: Path): GraphQLSchema {
    return unwiredSchema(SchemaParser().parse(path.reader()))
}

fun unwiredSchema(types: TypeDefinitionRegistry): GraphQLSchema {
    val interfaces = types.types().values.filterIsInstance<InterfaceTypeDefinition>()
    return SchemaGenerator().makeExecutableSchema(
        types,
        RuntimeWiring.newRuntimeWiring()
            .apply {
                for (iface in interfaces) {
                    type(iface.name) { builder -> builder.typeResolver { error("TypeResolver in unwired schema called") } }
                }
            }
            .build()
    )
}

fun String.toCamelCase(): String {
    return split('-', '_').joinToString(separator = "") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
