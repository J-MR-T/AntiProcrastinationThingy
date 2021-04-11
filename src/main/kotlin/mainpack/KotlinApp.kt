import androidx.compose.foundation.ExperimentalFoundationApi
import gui.KotlinGUI
import io.PersistenceHelper

@ExperimentalFoundationApi
fun main() {
    PersistenceHelper.startApp()
    KotlinGUI().getWindow()
}