package dev.twarner.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import graphql.GraphQLContext
import graphql.language.FieldDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.Type
import graphql.language.TypeDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.*
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.reader

fun generateResolverStubs(schemaFile: Path, outputDir: Path, packageName: String) {
    val types = SchemaParser().parse(schemaFile.reader())

    ResolverStubsGenerator(schemaFile.nameWithoutExtension, packageName)
        .generateFile(types)
        .writeTo(outputDir)
}

private class ResolverStubsGenerator(
    schemaFileName: String,
    val packageName: String,
) {
    private val namePrefix = schemaFileName.toCamelCase()

    fun generateFile(types: TypeDefinitionRegistry): FileSpec {
        val file = FileSpec.builder(packageName, "${namePrefix}Resolvers")
            .addKotlinDefaultImports()
        for (type in types.types().values) {
            file.addType(generateResolverStubForType(type))
        }
        file.addFunction(generateSchemaBuilderFunction(types))
        return file.build()
    }

    private fun generateResolverStubForType(type: TypeDefinition<*>): TypeSpec {
        return when (type) {
            is ObjectTypeDefinition -> generateResolverStubForObjectType(type)
            //is graphql.language.InterfaceTypeDefinition -> return generateResolverStubForInterfaceType(type)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    private fun generateResolverStubForObjectType(type: ObjectTypeDefinition): TypeSpec {
        val typeBuilder = TypeSpec.interfaceBuilder("${namePrefix}${type.name}Resolver")
        for (field in type.fieldDefinitions) {
            typeBuilder.addFunction(generateResolverFunction(field))
        }
        return typeBuilder.build()
    }

    private fun generateResolverFunction(field: FieldDefinition): FunSpec {
        val funBuilder = FunSpec.builder("get${field.name.replaceFirstChar { it.uppercaseChar() }}")
            .addModifiers(KModifier.ABSTRACT)
            .addParameter("context", GraphQLContext::class)
        for (arg in field.inputValueDefinitions) {
            funBuilder.addParameter(arg.name, arg.type.typeName())
        }
        return funBuilder.returns(field.type.typeName()).build()
    }

    private fun generateSchemaBuilderFunction(types: TypeDefinitionRegistry): FunSpec {
        val funBuilder = FunSpec.builder("create${namePrefix}Schema")
            .addParameter("resolver", ClassName(packageName, "${namePrefix}QueryResolver"))
            .returns(GraphQLSchema::class)

        val schemaStr = SchemaPrinter(SchemaPrinter.Options.defaultOptions().includeDirectives(SchemaPrinter.ExcludeGraphQLSpecifiedDirectivesPredicate))
            .print(unwiredSchema(types))
        funBuilder.addStatement("val types = %T().parse(%S)", SchemaParser::class, schemaStr)

        val wiringBuilder = CodeBlock.builder()
        wiringBuilder.add("val wiring = %T.newRuntimeWiring()\n", RuntimeWiring::class).indent()
        for (type in types.types().values) {
            if (type is ObjectTypeDefinition) {
                wiringBuilder.add(".type(%S) { wiring -> \n", type.name).indent()
                for (field in type.fieldDefinitions) {
                    wiringBuilder.add("wiring.dataFetcher(%S) {\n", field.name).indent()
                    val receiver = if (type.name == "Query") "resolver" else "it.getSource<${namePrefix}${type.name}Resolver>()"
                    wiringBuilder.add("$receiver.get${field.name.replaceFirstChar { it.uppercaseChar() }}(it.graphQlContext")
                    for (arg in field.inputValueDefinitions) {
                        wiringBuilder.add(", it.getArgument(%S)", arg.name)
                    }
                    wiringBuilder.add(")\n").unindent().add("}\n")
                }
                wiringBuilder.unindent().add("}\n")
            }
        }
        wiringBuilder.add(".build()\n").unindent()
        funBuilder.addCode(wiringBuilder.build())

        funBuilder.addStatement("return %T().makeExecutableSchema(types, wiring)", SchemaGenerator::class)

        return funBuilder.build()
    }

    private fun Type<*>.typeName(): TypeName {
        return when (this) {
            is ListType -> List::class.asTypeName().parameterizedBy(type.typeName()).copy(nullable = true)
            is NonNullType -> type.typeName().copy(nullable = false)
            is graphql.language.TypeName -> when (name) {
                "String", "ID" -> STRING
                "Int" -> INT
                "Float" -> DOUBLE
                "Boolean" -> BOOLEAN
                else -> ClassName(packageName, "${namePrefix}${name}Resolver")
            }.copy(nullable = true)
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }
    }
}
