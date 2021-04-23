package io

import gui.colors.MyColors

typealias OptionManipulationFunction = (String?, ArgParser.CmdOptions) -> Unit
typealias Option = Pair<Regex, OptionManipulationFunction>

class ArgParser(override val args: Array<String>) : CommandlineArgumentParser<ArgParser.CmdOptions> {

    override var possibleArguments: Map<Regex, OptionManipulationFunction> =
        mapOf(
            Option("theme") { value: String?, options: CmdOptions ->
                themes[value?.findAnyOf(themes.keys)?.second]?.let { options.colors = it }
            },
            Option("width|w") { value: String?, options: CmdOptions ->
                value?.toIntOrNull()?.let { options.width = it }
            },
            Option("height|") { value: String?, options: CmdOptions ->
                value?.toIntOrNull()?.let { options.height = it }
            },
            Option("volume") { value: String?, options: CmdOptions ->
                value?.toDoubleOrNull()?.let { options.volume = it }
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
        var width: Int = 1280,
        var height: Int = 720,
        var volume: Double = PersistenceHelper.loadFromFile(PersistenceHelper.DEFAULT_VOLUME_PATH) ?: 0.1,
    ) : io.CmdOptions

}