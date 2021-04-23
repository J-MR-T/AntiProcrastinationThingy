package mainpack

import gui.KotlinGUI
import io.ArgParser
import io.getCmdOptions

fun main(args: Array<String>) {
    val options = ArgParser(args).getCmdOptions()
    KotlinGUI.options = options
    KotlinGUI.show()
}