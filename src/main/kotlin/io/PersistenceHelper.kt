package io

import com.google.gson.Gson
import processes.ProcessHandler
import processes.ProcessHandler.computeReducedProcessList
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

object PersistenceHelper {
    private val DEFAULT_BLACKLIST_SET_PATH = Path.of("res", "blacklisted.json")
    private val DEFAULT_VOLUME_PATH = Path.of("res", "volume.txt")
    private val DEFAULT_HIDDEN_PROCESSES_PATH = Path.of("res", "hidden.json")
    private val gson = Gson()

    @JvmOverloads
    @Throws(FileNotFoundException::class)
    fun readBlacklistSet(path: Path = DEFAULT_BLACKLIST_SET_PATH): MutableSet<String> {
        return gson.fromJson(InputStreamReader(FileInputStream(path.toFile())), mutableSetOf<String>().javaClass)
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun writeBlacklistSet(set: Set<String?>?, path: Path? = DEFAULT_BLACKLIST_SET_PATH) {
        Files.writeString(path, gson.toJson(set))
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun writeVolume(volume: Double, path: Path? = DEFAULT_VOLUME_PATH) {
        Files.writeString(path, (volume * 100).toString())
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun writeHiddenProcesses(hiddenProcesses: List<String?>?, path: Path? = DEFAULT_HIDDEN_PROCESSES_PATH) {
        Files.writeString(path, gson.toJson(hiddenProcesses))
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun readHiddenProcesses(path: Path = DEFAULT_HIDDEN_PROCESSES_PATH): MutableList<String> {
        return gson.fromJson(InputStreamReader(FileInputStream(path.toFile())), mutableListOf<String>().javaClass)
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun startApp() {
        val blacklisted = readBlacklistSet()
        ProcessHandler.blacklisted = blacklisted
        val hiddenProcesses = readHiddenProcesses()
        ProcessHandler.hiddenProcesses = hiddenProcesses
        computeReducedProcessList(true)
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun stopApp(volume: Double? = null) {
        writeBlacklistSet(ProcessHandler.blacklisted)
        volume?.let { writeVolume(it) }
        writeHiddenProcesses(ProcessHandler.hiddenProcesses)
    }

    @Throws(IOException::class)
    fun <T> saveObjectToFile(`object`: T, path: Path?) {
        Files.writeString(path, gson.toJson(`object`))
    }

    @Throws(IOException::class)
    fun <T> readObjectFromFile(tClass: Class<T>?, path: Path?): T {
        return gson.fromJson(Files.readString(path), tClass)
    }
}