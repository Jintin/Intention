package com.jintin.intention.compiler

import com.google.auto.service.AutoService
import com.jintin.intention.*
import com.squareup.kotlinpoet.*
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypeException
import javax.tools.Diagnostic
import kotlin.Boolean
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String


@AutoService(Processor::class)
class IntentionProcessor : AbstractProcessor() {

    private lateinit var filer: Filer
    private lateinit var messager: Messager

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun getSupportedAnnotationTypes() =
        setOf<String>(Intention::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        try {
            for (element in roundEnvironment.getElementsAnnotatedWith(Intention::class.java)) {
                if (element.kind != ElementKind.INTERFACE) {
                    error(ERROR_NOT_INTERFACE, element)
                    return true
                }

                val name = element.simpleName.toString() + "Util"
                val kotlinFile = parseFile(element, name)

                if (element is TypeElement) {
                    val typeSpec = parseClass(element, name)
                    kotlinFile.addType(typeSpec)
                }
                kotlinFile.build().writeTo(filer)
            }
        } catch (ex: Exception) {
            error(ex.toString())
        }
        return true
    }

    private fun parseFile(element: Element, name: String): FileSpec.Builder {
        val pkg = processingEnv.elementUtils.getPackageOf(element)
        return FileSpec.builder(pkg.toString(), name)
    }

    private fun parseClass(element: TypeElement, name: String): TypeSpec {
        val typeSpec = TypeSpec.objectBuilder(name)
            .addSuperinterface(element.asTypeName())
        val activity = parseActivity(element)
        element.enclosedElements.filterIsInstance<ExecutableElement>()
            .map { parseMethod(it, activity) }
            .forEach(typeSpec::addFunction)
        return typeSpec.build()
    }

    private fun parseExtra(
        element: VariableElement,
        typeMap: MutableMap<String, Pair<String, String>>
    ) {
        val name = element.simpleName.toString()
        element.getAnnotation(Extra::class.java)?.let {
            typeMap[name] = Pair("Extra", it.value)
        } ?: element.getAnnotation(IntegerArrayListExtra::class.java)?.let {
            typeMap[name] = Pair("IntegerArrayListExtra", it.value)
        } ?: element.getAnnotation(ParcelableArrayListExtra::class.java)?.let {
            typeMap[name] = Pair("ParcelableArrayListExtra", it.value)
        } ?: element.getAnnotation(StringArrayListExtra::class.java)?.let {
            typeMap[name] = Pair("StringArrayListExtra", it.value)
        } ?: element.getAnnotation(CharSequenceArrayListExtra::class.java)?.let {
            typeMap[name] = Pair("CharSequenceArrayListExtra", it.value)
        } ?: run {
            error(ERROR_PARAMETER_KEY, element)
        }
    }

    private fun parseMethod(element: ExecutableElement, activity: ClassName): FunSpec {
        val builder = FunSpec.builder(element.simpleName.toString())
//            .addAnnotation(JvmStatic::class.java)
            .addModifiers(KModifier.OVERRIDE)
            .returns(TYPE_INTENT)
        val typeMap: MutableMap<String, Pair<String, String>> = mutableMapOf()
        val context = parseParameters(element, typeMap, builder)

        builder.addStatement(
            "val intent = %T($context, ${activity.canonicalName}::class.java)",
            TYPE_INTENT
        )
        builder.parameters.forEach {
            if (it.name != context) {
                val (type, key) = typeMap[it.name] ?: throw RuntimeException()
                val statement = "intent.put$type(\"$key\", ${it.name})"
                if (it.type.isNullable) {
                    builder.beginControlFlow("if (${it.name} != null)")
                    builder.addStatement(statement)
                    builder.endControlFlow()
                } else {
                    builder.addStatement(statement)
                }
            }
        }
        builder.addStatement("return intent")
        return builder.build()
    }

    private fun parseParameters(
        element: ExecutableElement,
        typeMap: MutableMap<String, Pair<String, String>>,
        builder: FunSpec.Builder
    ): String {
        var context = ""
        element.parameters.forEach {
            val parameter = ParameterSpec(it.simpleName.toString(), it.asTypeName())
            if (parameter.type.toString() == "android.content.Context") {
                context = parameter.name
            } else {
                parseExtra(it, typeMap)
            }
            addParameter(parameter, builder)
        }
        if (context.isEmpty()) {
            error(ERROR_NULL_CONTEXT, element)
        }
        return context
    }

    private val transformTypeMap = mapOf(
        "java.lang.String" to String::class.asTypeName(),
        "java.lang.Byte" to Byte::class.asTypeName(),
        "java.lang.Character" to Char::class.asTypeName(),
        "java.lang.Short" to Short::class.asTypeName(),
        "java.lang.Integer" to Int::class.asTypeName(),
        "java.lang.Long" to Long::class.asTypeName(),
        "java.lang.Float" to Float::class.asTypeName(),
        "java.lang.Double" to Double::class.asTypeName(),
        "java.lang.Boolean" to Boolean::class.asTypeName(),
    )

    private fun addParameter(parameter: ParameterSpec, builder: FunSpec.Builder) {
        // handle Type in Kotlin way if need
        val key = parameter.type.toString().removeSuffix("?")
        println("keykkkkkk:" + key)
        transformTypeMap[key]?.let {
            builder.addParameter(
                parameter.name,
                if (parameter.type.isNullable) {
                    it.copy(nullable = true)
                } else {
                    it
                }
            )
        } ?: run {
            builder.addParameter(parameter)
        }
    }

    private fun parseActivity(element: Element): ClassName {
        val annotation = element.getAnnotation(Intention::class.java)
        return try {
            val simpleName = annotation.value.simpleName.orEmpty()
            ClassName(
                annotation.value.qualifiedName.toString().replace(".$simpleName", ""),
                simpleName
            )
        } catch (e: MirroredTypeException) {
            val rawString = e.typeMirror.toString()
            val dotPosition = rawString.lastIndexOf(".")
            val packageName = rawString.substring(0, dotPosition)
            val className = rawString.substring(dotPosition + 1)
            ClassName(packageName, className)
        }
    }

    @Suppress("DEPRECATION")
    private fun Element.asTypeName(): TypeName {
        val annotation = this.getAnnotation(Nullable::class.java)
        val typeName = this.asType().asTypeName()
        return if (annotation != null) typeName.copy(nullable = true) else typeName
    }

    private fun error(message: String, element: Element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }

    companion object {
        const val ERROR_NOT_INTERFACE = "@Intention only work with interface"
        const val ERROR_NULL_CONTEXT = "Context should not be null"
        const val ERROR_PARAMETER_KEY = "Parameter is not annotate with Extra"

        val TYPE_INTENT = ClassName("android.content", "Intent")
    }

}