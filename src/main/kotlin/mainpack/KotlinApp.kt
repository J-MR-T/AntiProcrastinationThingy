package mainpack

import gui.KotlinGUI
import io.commandline.ArgParser
import io.commandline.getCmdOptions

fun main(args: Array<String>) {
    val options = ArgParser(args).getCmdOptions()
    KotlinGUI.options = options
    KotlinGUI.show()
}