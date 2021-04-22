package io

import gui.colors.MyColors

typealias OptionManipulationFunction = (String?, ArgParser.CmdOptions) -> Unit
typealias Option = Pair<String, OptionManipulationFunction>

class ArgParser(override val args: Array<String>) : CommandlineArgumentParser<ArgParser.CmdOptions> {

    override var possibleArguments: Map<String, OptionManipulationFunction> =
        mapOf(
            Option("theme") { value: String?, options: CmdOptions ->
                themes[value?.findAnyOf(themes.keys)?.second]?.let { options.colors = it }
            },
        )

    private val themes: Map<String, MyColors> = mapOf(
        "dark" to MyColors.DARK,
        "awesome" to MyColors.AWESOME_MAGNET,
        "default" to MyColors.AWESOME_MAGNET,
        "magnet" to MyColors.AWESOME_MAGNET,
        "shitty" to MyColors.LIGHT,
        "light" to MyColors.LIGHT,
    )

    /**
     * Arguments:   -colors=default @param colors=MyColors.DARK
     *              -colors=awesome @param colors=MyColors.AWESOME_MAGNET
     */
    class CmdOptions(
        var colors: MyColors = MyColors.AWESOME_MAGNET,
    ) : io.CmdOptions

}