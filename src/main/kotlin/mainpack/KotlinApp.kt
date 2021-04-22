package mainpack

import androidx.compose.runtime.Composable
import gui.KotlinGUI
import io.*

fun main(args: Array<String>) {
    val options = ArgParser(args).getCmdOptions()
    KotlinGUI(options.colors).show()
}