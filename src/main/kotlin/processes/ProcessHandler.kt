package processes

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import processes.implementations.ProcessIdentifier
import processes.implementations.RunningProcess
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.toList

object ProcessHandler {
    val DEFAULT_HIDDEN_PROCESSES: Set<String> = setOf(
        "System32", "Nvidia", "SystemApps", "wallpaper",
        "Razer", "Native Instruments", "xboxGam", "Microsoft.ZuneVideo", "Settings", "GWSL",
        "Keyboard Chattering Fix", "YourPhone", "webhelper", "Driver", "Gaomon", "Git", "fsnotifier",
        "manager", "launcher", "daemon", "system", "proxy", "kde","qt"
    )
    var blacklisted: SnapshotStateList<Process> = mutableStateListOf()
    var hiddenProcesses: MutableList<Process> = resetHiddenProcesses()

    val afterKill: (RunningProcess) -> Unit = { proc: RunningProcess ->
        allProcesses.remove(proc.command)
    }

    //:blobdoubt:
    private var allProcesses: MutableMap<String, RunningProcess> =
        ProcessHandle.allProcesses().asSequence().associate { handle ->
            (handle.info().command().orElse("")) to (RunningProcess(handle, afterKill))
        }.toMutableMap()

    @JvmOverloads
    fun computeReducedProcessList(
        user: String = System.getProperty("user.name"),
        cmdBlacklist: List<Process> = hiddenProcesses,
    ): Stream<RunningProcess>? {
        val runningProcesses =
            ProcessHandle.allProcesses().asSequence()
                .associateBy { processHandle -> processHandle.info().command().orElse("") }

        synchronized(allProcesses) {
            runningProcesses.values.parallelStream().forEach { handle ->
                val handleCommand = handle.info().command().orElse("")
                allProcesses.computeIfAbsent(handleCommand) {
                    RunningProcess(handle, afterKill)
                }
            }
        }
        val user = user.toLowerCase()
        synchronized(cmdBlacklist) {
            return allProcesses.values
                .parallelStream()
                .distinct()
                .filter { proc ->
                    proc.user.contains(user)
                }
                .filter { proc ->
                    cmdBlacklist.none { otherProcess ->
                        proc == otherProcess || proc.command.contains(
                            otherProcess.stringRepresentation,
                            ignoreCase = true
                        )
                    }
                }
                .filter {
                    runningProcesses.keys.contains(it.command)
                }
        }
    }

    fun computeFilteredProcessList(stream: Stream<RunningProcess>? = computeReducedProcessList()): List<RunningProcess> {
        return stream?.filter { proc: Process ->
            !blacklisted.contains(proc)
        }?.toList() ?: emptyList()
    }

    val disallowedProcessesThatAreRunning: List<RunningProcess>
        get() {
            synchronized(blacklisted) {
                return computeReducedProcessList()
                    ?.filter { process: Process ->
                        blacklisted.contains(process)
                    }?.toList() ?: emptyList()
            }
        }

    fun resetHiddenProcesses(): MutableList<Process> {
        hiddenProcesses = DEFAULT_HIDDEN_PROCESSES
            .map { name -> ProcessIdentifier(stringRepresentation = name) }
            .toMutableStateList()
        return hiddenProcesses.toMutableStateList()
    }
}
