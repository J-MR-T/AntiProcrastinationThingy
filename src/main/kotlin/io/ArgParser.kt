package io

import gui.colors.MyColors

class ArgParser(override val args: Array<String>) : CommandlineArgumentParser<ArgParser.CmdOptions> {

    override var possibleArguments: Map<String, (String?, CmdOptions) -> Unit> =
        mapOf(
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

    /**
     * Arguments:   -gui=old/-gui=javafx @param javafxGUI = true
     *              -gui=new/-gui=compose @param javafxGUI = false
     *              -colors=default @param colors=MyColors.DEFAULT
     *              -colors=awesome @param colors=MyColors.AWESOME_MAGNET
     */
    class CmdOptions(var javafxGUI: Boolean = false, var colors: MyColors = MyColors.DEFAULT) : io.CmdOptions

}