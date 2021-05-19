package io.serialization

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import processes.Process
import processes.ProcessHandler
import processes.ProcessHandler.computeReducedProcessList
import java.nio.file.Files
import java.nio.file.Path

object PersistenceHelper {
    val DEFAULT_BLACKLIST_SET_PATH: Path = Path.of("res", "blacklisted.json")
    val DEFAULT_VOLUME_PATH: Path = Path.of("res", "volume.txt")
    val DEFAULT_HIDDEN_PROCESSES_PATH: Path = Path.of("res", "hidden.json")

    fun startApp() {
        val blacklisted =
            loadFromFile<List<Process>>(DEFAULT_BLACKLIST_SET_PATH)
        ProcessHandler.blacklisted = blacklisted?.toMutableStateList() ?: mutableStateListOf()
        val hiddenProcesses = loadFromFile<MutableList<Process>>(DEFAULT_HIDDEN_PROCESSES_PATH)
        ProcessHandler.hiddenProcesses = hiddenProcesses?.toMutableStateList() ?: ProcessHandler.resetHiddenProcesses()
        computeReducedProcessList()
    }

    fun stopApp(volume: Double? = null) {
        saveToFile(ProcessHandler.blacklisted.toList(), DEFAULT_BLACKLIST_SET_PATH)
        volume?.let { saveToFile(it, DEFAULT_VOLUME_PATH) }
        saveToFile(ProcessHandler.hiddenProcesses, DEFAULT_HIDDEN_PROCESSES_PATH)
    }

    inline fun <reified T> saveToFile(t: T, path: Path) {
        try {
            Files.writeString(path, Json.encodeToString(t))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inline fun <reified T> loadFromFile(path: Path): T? {
        return try {
            Json.decodeFromString(Files.readString(path))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}