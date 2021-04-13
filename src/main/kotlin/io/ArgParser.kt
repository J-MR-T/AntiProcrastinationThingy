package io

import gui.colors.MyColors

class ArgParser(
    private val args: Array<String>,
) {
    private val possibleArguments: Map<String, (value: String?, options: CmdOptions) -> Unit> = mapOf(
        Pair("gui") { value: String?, options: CmdOptions ->
            value?.findAnyOf(oldGUIKeywords, ignoreCase = true)?.let { options.javafxGUI = true }
            value?.findAnyOf(newGUIKeywords, ignoreCase = true)?.let { options.javafxGUI = false }
        },
        Pair("theme") { value: String?, options: CmdOptions ->
            themes[value?.findAnyOf(themes.keys)?.second]?.let { options.colors = it }
        },
    )

    private val newGUIKeywords = listOf("new", "compose")
    private val oldGUIKeywords = listOf("old", "javafx")
    private val themes: Map<String, MyColors> = mapOf(
        Pair("default", MyColors.DEFAULT),
        Pair("awesome", MyColors.AWESOME_MAGNET),
        Pair("magnet", MyColors.AWESOME_MAGNET),
    )

    fun getCmdOptions(): CmdOptions {
        val options = CmdOptions()
        possibleArguments.forEach { entry ->
            val value: String? = getSingleOption(entry.key)
            entry.value(value, options)
        }
        return options
    }

    private fun getSingleOption(name: String): String? {
        return args.find { it.startsWith("-$name=") }?.split("=")?.get(1)?.toLowerCase()
    }

    /**
     * Arguments:   -gui=old/-gui=javafx @param javafxGUI = true
     *              -gui=new/-gui=compose @param javafxGUI = false
     *              -colors=default @param colors=MyColors.DEFAULT
     *              -colors=awesome @param colors=MyColors.AWESOME_MAGNET
     */
    class CmdOptions(var javafxGUI: Boolean = false, var colors: MyColors = MyColors.DEFAULT)
}