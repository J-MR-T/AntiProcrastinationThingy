package mainpack

import com.sun.javafx.application.LauncherImpl
import gui.KotlinGUI
import io.*

fun main(args: Array<String>) {
    val options = ArgParser(args).getCmdOptions()
    if (options.javafxGUI) {
        LauncherImpl.launchApplication(App::class.java, args)
    } else {
        PersistenceHelper.startApp()
        KotlinGUI(options.colors).getWindow()
    }
}