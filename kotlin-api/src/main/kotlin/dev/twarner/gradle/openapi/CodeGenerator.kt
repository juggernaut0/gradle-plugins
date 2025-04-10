package dev.twarner.gradle.openapi

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Schema

fun nameSchemas(model: OpenAPI): List<Schema<Any>> {
    val allSchemas = mutableListOf<Schema<Any>>()
    for ((name, schema) in model.components.schemas) {
        if (schema.name == null) {
            schema.name = name
            allSchemas.add(schema)
        }
    }
    for (op in model.paths.flatMap { it.value.readOperationsMap().values }) {
        val camelCaseOpId = op.camelCaseOperationId()
        val requestSchema = op.requestBody?.content?.get("application/json")?.schema
        if (requestSchema != null && requestSchema.name == null) {
            requestSchema.name = "${camelCaseOpId}Request"
            allSchemas.add(requestSchema)
        }
        for ((code, response) in op.responses) {
            val responseSchema = response.content?.get("application/json")?.schema
            if (responseSchema != null && responseSchema.name == null) {
                responseSchema.name = "${camelCaseOpId}${code}Response"
                allSchemas.add(responseSchema)
            }
        }
    }
    return allSchemas
}

fun createModelsFile(packageName: String, schemas: List<Schema<Any>>): FileSpec {
    val file = FileSpec.builder(packageName, "Models").addKotlinDefaultImports()
    for (schema in schemas) {
        file.appendModel(packageName, schema)
    }
    return file.build()
}

fun FileSpec.Builder.appendModel(packageName: String, schema: Schema<Any>) {
    requireNotNull(schema.name) { "Schema must have a name" }

    val type = TypeSpec.classBuilder(schema.name)
        .addModifiers(KModifier.DATA)
        .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
    val ctor = FunSpec.constructorBuilder()
    val required = schema.required?.toSet().orEmpty()
    for ((name, property) in schema.properties.orEmpty()) {
        val nullable = name !in required
        val typeName = property.typeName(packageName).copy(nullable = nullable)
        ctor.addParameter(name, typeName)
        type.addProperty(PropertySpec.builder(name, typeName).initializer(name).build())
    }
    type.primaryConstructor(ctor.build())
    addType(type.build())
}

private fun Schema<*>.typeName(packageName: String): TypeName {
    return when (type) {
        "string" -> {
            when (format) {
                "date" -> ClassName("kotlinx.datetime", "LocalDate")
                "date-time" -> ClassName("kotlinx.datetime", "Instant")
                else -> String::class.asTypeName()
            }
        }
        "integer" -> {
            if (format == "int64") {
                Long::class.asTypeName()
            } else {
                Int::class.asTypeName()
            }
        }
        "number" -> Double::class.asTypeName()
        "boolean" -> Boolean::class.asTypeName()
        "array" -> {
            val itemType = items?.typeName(packageName) ?: error("Array must have items")
            List::class.asTypeName().parameterizedBy(itemType)
        }
        else -> ClassName(packageName, name)
    }
}

fun createRoutesFile(packageName: String, paths: Paths): FileSpec {
    val routesFile = FileSpec.builder(packageName, "Routes")
    val apiRouteType = ClassName("multiplatform.api", "ApiRoute")
    val apiRouteWithBodyType = ClassName("multiplatform.api", "ApiRouteWithBody")
    val methodClassName = ClassName("multiplatform.api", "Method")
    val pathOfName = MemberName("multiplatform.api", "pathOf")
    val builtinSerializerMethod = MemberName("kotlinx.serialization.builtins", "serializer")
    for ((path, pathItem) in paths) {
        for ((method, operation) in pathItem.readOperationsMap()) {
            val paramsClassName = if (operation.parameters.orEmpty().isNotEmpty()) {
                val className = ClassName(packageName, "${operation.camelCaseOperationId()}Params")
                val paramsType = TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.DATA)
                    .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                val ctor = FunSpec.constructorBuilder()
                for (param in operation.parameters) {
                    if (param.schema.type != "string") {
                        throw IllegalArgumentException("Issue with operation ${operation.operationId} Only string parameters are supported")
                    }
                    val typeName = String::class.asTypeName()
                    ctor.addParameter(param.name, typeName)
                    paramsType.addProperty(PropertySpec.builder(param.name, typeName).initializer(param.name).build())
                }
                paramsType.primaryConstructor(ctor.build())
                routesFile.addType(paramsType.build())
                className
            } else {
                Unit::class.asTypeName()
            }

            val requestSchema = operation.requestBody?.content?.get("application/json")?.schema
            val requestTypeName = requestSchema?.let { ClassName(packageName, it.name) }

            val responseSchema = operation.responses.entries
                .firstOrNull { it.key.startsWith("2") }
                ?.value
                ?.content
                ?.get("application/json")
                ?.schema
            val responseTypeName = if (responseSchema != null) {
                ClassName(packageName, responseSchema.name)
            } else {
                Unit::class.asTypeName()
            }

            val propertyType = if (requestTypeName == null) {
                apiRouteType.parameterizedBy(paramsClassName, responseTypeName)
            } else {
                apiRouteWithBodyType.parameterizedBy(paramsClassName, requestTypeName, responseTypeName)
            }

            val methodName = MemberName(methodClassName, method.name.uppercase())

            fun CodeBlock.Builder.addSerializer(typeName: TypeName): CodeBlock.Builder {
                add("%T.", typeName)
                if (typeName == Unit::class.asTypeName()) {
                    add("%M", builtinSerializerMethod)
                } else {
                    add("serializer")
                }
                return add("()")
            }

            val initializer = CodeBlock.builder()
                .add("ApiRoute(%M, %M(", methodName, pathOfName)
                .addSerializer(paramsClassName)
                .add(", %S), ", path)
                .addSerializer(responseTypeName)
                .also {
                    if (requestTypeName != null) {
                        it.add(", ").addSerializer(requestTypeName)
                    }
                }
                .add(")")

            val property = PropertySpec.builder(operation.operationId, propertyType)
                .initializer(initializer.build())
                .build()
            routesFile.addProperty(property)
        }
    }
    return routesFile.build()
}

fun printSpec(model: OpenAPI, logger: (String) -> Unit) {
    for ((path, pathItem) in model.paths) {
        logger("Path: $path")
        for ((method, operation) in pathItem.readOperationsMap()) {
            logger("Method: $method")
            logger("Operation: $operation")
        }
    }
    for ((t, u) in model.components.schemas) {
        logger("$t $u")
    }
}

private fun Operation.camelCaseOperationId(): String {
    return operationId.replaceFirstChar { it.uppercaseChar() }
}
