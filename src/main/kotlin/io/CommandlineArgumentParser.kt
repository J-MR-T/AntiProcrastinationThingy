package io

import kotlin.reflect.KClass
import kotlin.reflect.KFunction


interface CommandlineArgumentParser<T : CmdOptions> {
    val args: Array<String>
    val possibleArguments: Map<String, (value: String?, options: T) -> Unit>

    private fun getSingleOption(name: String): String? {
        return args.find { it.startsWith("-$name=") }?.split("=")?.get(1)?.toLowerCase()
    }

    fun getCmdOptionsInternal(kClass: KClass<T>): T {
        val options: T =
            try {
                kClass.constructors.firstOrNull { constructor ->
                    constructor.parameters.isEmpty() || constructor.parameters.all { kParameter -> kParameter.isOptional }
                }
                    ?.callBy(emptyMap())
                    ?: throw NoZeroArgumentConstructorException(kClass.constructors)
            } catch (e: NoZeroArgumentConstructorException) {
                throw e
            } catch (e: Exception) {
                throw NoZeroArgumentConstructorException(kClass.constructors)
            }
        possibleArguments.forEach { entry ->
            val value: String? = getSingleOption(entry.key)
            entry.value(value, options)
        }
        return options
    }

}

inline fun <reified T : CmdOptions> CommandlineArgumentParser<T>.getCmdOptions(): T {
    return getCmdOptionsInternal(T::class);
}

class NoZeroArgumentConstructorException(constructors: Collection<KFunction<Any>>? = null) :
    Exception(
        if (constructors != null)
            """
                Couldn't find a matching constructor that can be called with Zero arguments, all constructors are: $constructors
            """.trimIndent()
        else
            "Couldn't find a matching constructor that can be called with Zero arguments"
    ) {
}
