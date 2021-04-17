package io

import gui.colors.MyColors

typealias OptionManipulationFunction = (String?, ArgParser.CmdOptions) -> Unit
typealias Option = Pair<String, OptionManipulationFunction>

class ArgParser(override val args: Array<String>) : CommandlineArgumentParser<ArgParser.CmdOptions> {

    override var possibleArguments: Map<String, OptionManipulationFunction> =
        mapOf(
            Option("gui") { value: String?, options: CmdOptions ->
                value?.findAnyOf(oldGUIKeywords, ignoreCase = true)?.let { options.javafxGUI = true }
                value?.findAnyOf(newGUIKeywords, ignoreCase = true)?.let { options.javafxGUI = false }
            },
            Option("theme") { value: String?, options: CmdOptions ->
                themes[value?.findAnyOf(themes.keys)?.second]?.let { options.colors = it }
            },
        )

    private val newGUIKeywords = listOf("new", "compose")
    private val oldGUIKeywords = listOf("old", "javafx")
    private val themes: Map<String, MyColors> = mapOf(
        "dark" to MyColors.DARK,
        "awesome" to MyColors.AWESOME_MAGNET,
        "default" to MyColors.AWESOME_MAGNET,
        "magnet" to MyColors.AWESOME_MAGNET,
        "shitty" to MyColors.LIGHT,
        "light" to MyColors.LIGHT,
    )

    /**
     * Arguments:   -gui=old/-gui=javafx @param javafxGUI = true
     *              -gui=new/-gui=compose @param javafxGUI = false
     *              -colors=default @param colors=MyColors.DARK
     *              -colors=awesome @param colors=MyColors.AWESOME_MAGNET
     */
    class CmdOptions(
        var javafxGUI: Boolean = false,
        var colors: MyColors = MyColors.AWESOME_MAGNET,
    ) : io.CmdOptions

}