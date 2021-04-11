import androidx.compose.foundation.ExperimentalFoundationApi
import io.PersistenceHelper
import processes.ProcessHandler

@ExperimentalFoundationApi
fun main() {
    ProcessHandler.blacklisted = PersistenceHelper.readBlacklistSet()
    ProcessHandler.hiddenProcesses = PersistenceHelper.readHiddenProcesses()
    KotlinGUI().getWindow()
}