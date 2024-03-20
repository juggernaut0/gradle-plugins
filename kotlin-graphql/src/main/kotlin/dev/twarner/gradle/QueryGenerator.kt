package dev.twarner.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.Scalars
import graphql.language.Field
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import graphql.language.SelectionSetContainer
import graphql.language.Type
import graphql.language.Value
import graphql.schema.*
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

fun generateQuery(queryFile: Path, validationSchemaFile: Path, outputDir: Path, packageName: String) {
    val execInput = ExecutionInput.newExecutionInput()
        .query(queryFile.readText())
        .build()
    val validationSchema = unwiredSchema(validationSchemaFile)
    val result = ParseAndValidate.parseAndValidate(validationSchema, execInput)
    // TODO handle errors
    val document = result.document
    val queryGenerator = QueryGenerator(queryFile.nameWithoutExtension, packageName, validationSchema)
    for (def in document.definitions) {
        if (def is OperationDefinition && def.operation == OperationDefinition.Operation.QUERY) {
            val file = queryGenerator.generateFile(def)
            file.writeTo(outputDir)
        }
    }
}

class QueryGenerator(
    queryFileName: String,
    private val packageName: String,
    private val validationSchema: GraphQLSchema,
) {
    private val namePrefix = queryFileName.toCamelCase()

    fun generateFile(def: OperationDefinition): FileSpec {
        val name = def.name ?: "${namePrefix}Query"
        val file = FileSpec.builder(packageName, name)
            .addKotlinDefaultImports()

        val type = TypeSpec.classBuilder(name)
            .addSuperinterface(MULTIPLATFORM_GRAPHQL_QUERY.parameterizedBy(ClassName(packageName, name, "Response")))
            .addProperty(PropertySpec.builder("queryString", String::class)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("%S", def.toQueryString())
                .build())

        val ctor = FunSpec.constructorBuilder()
        val variablesInitializer = CodeBlock.builder().add("%M(\n", MemberName("kotlin.collections", "mapOf"))
        for (variable in def.variableDefinitions) {
            ctor.addParameter(variable.name, variable.type.typeName())
            variablesInitializer.add("%S to %N,\n", variable.name, variable.name)
        }
        variablesInitializer.add(")")
        type.addProperty(PropertySpec.builder("variables", MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
                .addModifiers(KModifier.OVERRIDE)
                .initializer(variablesInitializer.build())
                .build())
            .primaryConstructor(ctor.build())

        type.addProperty(PropertySpec.builder("responseDeserializer", DESERIALIZATION_STRATEGY.parameterizedBy(ClassName(packageName, name, "Response")))
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%T.serializer()", ClassName(packageName, name, "Response"))
            .build())

        type.addType(generateResponseClass("Response", def, validationSchema.queryType, listOf(name)))

        file.addType(type.build())

        return file.build()
    }

    private fun generateResponseClass(
        name: String,
        def: SelectionSetContainer<*>,
        schemaType: GraphQLObjectType,
        parentFullName: List<String>,
    ): TypeSpec {
        val fullName = parentFullName + name
        val type = TypeSpec.classBuilder(name)
            .addAnnotation(SERIALIZABLE)
            .addModifiers(KModifier.DATA)
        val ctor = FunSpec.constructorBuilder()
        for (selection in def.selectionSet.selections) {
            when (selection) {
                is Field -> {
                    val fieldType = schemaType.getField(selection.name).type
                    val typeName = fieldToResponseType(selection, fieldType, type, fullName)
                    ctor.addParameter(selection.name, typeName)
                    type.addProperty(PropertySpec.builder(selection.name, typeName)
                        .initializer(selection.name)
                        .build())
                }
                else -> throw IllegalArgumentException("Unsupported selection: $selection")
            }
        }
        type.primaryConstructor(ctor.build())
        return type.build()
    }

    private fun fieldToResponseType(
        field: Field,
        fieldType: GraphQLType,
        parentSpec: TypeSpec.Builder,
        parentFullName: List<String>,
    ): TypeName {
        return when (fieldType) {
            is GraphQLScalarType -> {
                when (fieldType) {
                    Scalars.GraphQLID, Scalars.GraphQLString -> STRING
                    Scalars.GraphQLInt -> INT
                    Scalars.GraphQLFloat -> DOUBLE
                    Scalars.GraphQLBoolean -> BOOLEAN
                    else -> throw IllegalArgumentException("Unsupported scalar type: $this")
                }.copy(nullable = true)
            }
            is GraphQLObjectType -> {
                val camelCaseName = field.name.replaceFirstChar { it.uppercaseChar() }
                parentSpec.addType(generateResponseClass(camelCaseName, field, fieldType, parentFullName))
                ClassName(packageName, parentFullName + camelCaseName).copy(nullable = true)
            }
            is GraphQLNonNull -> {
                fieldToResponseType(field, fieldType.wrappedType, parentSpec, parentFullName).copy(nullable = false)
            }
            is GraphQLList -> {
                val wrapped = fieldToResponseType(field, fieldType.wrappedType, parentSpec, parentFullName)
                LIST.parameterizedBy(wrapped).copy(nullable = true)
            }
            else -> throw IllegalArgumentException("Unsupported field type $fieldType of class ${fieldType::class}")
        }
    }
}

private fun OperationDefinition.toQueryString(): String {
    return buildString {
        if (!variableDefinitions.isNullOrEmpty()) {
            append("query(")
            append(variableDefinitions.joinToString(", ") { "$${it.name}: ${it.type.toQueryString()}" })
            append(") ")
        }
        appendSelectionSet(selectionSet, indent = 0)
        appendLine()
    }
}

private fun StringBuilder.appendSelectionSet(ss: SelectionSet, indent: Int) {
    append("{\n")
    for (selection in ss.selections) {
        append("  ".repeat(indent + 1))
        when (selection) {
            is Field -> {
                append(selection.name)
                if (selection.arguments.isNotEmpty()) {
                    append("(")
                    append(selection.arguments.joinToString(", ") { "${it.name}: ${it.value.toQueryString()}" })
                    append(")")
                }
                if (selection.selectionSet != null) {
                    append(" ")
                    appendSelectionSet(selection.selectionSet, indent + 1)
                }
            }
            else -> throw IllegalArgumentException("Unsupported selection: $selection")
        }
        appendLine()
    }
    append("  ".repeat(indent)).append("}")
}

private fun Type<*>.toQueryString(): String {
    return when (this) {
        is ListType -> "[${type.toQueryString()}]"
        is NonNullType -> type.toQueryString() + "!"
        is graphql.language.TypeName -> name
        else -> throw IllegalArgumentException("Unsupported type: $this")
    }
}

private fun Value<*>.toQueryString(): String {
    return when (this) {
        is graphql.language.StringValue -> "\"$value\""
        is graphql.language.IntValue -> value.toString()
        is graphql.language.FloatValue -> value.toString()
        is graphql.language.BooleanValue -> isValue.toString()
        is graphql.language.EnumValue -> name
        is graphql.language.VariableReference -> "$$name"
        else -> throw IllegalArgumentException("Unsupported value: $this of class ${this::class}")
    }
}

private fun Type<*>.typeName(): TypeName {
    return when (this) {
        is ListType -> LIST.parameterizedBy(type.typeName()).copy(nullable = true)
        is NonNullType -> type.typeName().copy(nullable = false)
        is graphql.language.TypeName -> when (name) {
            "String", "ID" -> STRING
            "Int" -> INT
            "Float" -> DOUBLE
            "Boolean" -> BOOLEAN
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }.copy(nullable = true)
        else -> throw IllegalArgumentException("Unsupported type: $this")
    }
}

private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
private val DESERIALIZATION_STRATEGY = ClassName("kotlinx.serialization", "DeserializationStrategy")
private val MULTIPLATFORM_GRAPHQL_QUERY = ClassName("multiplatform.graphql", "GraphQLQuery")
