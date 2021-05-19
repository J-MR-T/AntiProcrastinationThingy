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
    val DEFAULT_HIDDEN_PROCESSES: MutableSet<String> = mutableSetOf(
        "System32", "Nvidia", "SystemApps", "wallpaper",
        "Razer", "Native Instruments", "xboxGam", "Microsoft.ZuneVideo", "Settings", "GWSL",
        "Keyboard Chattering Fix", "YourPhone", "webhelper", "Driver", "Gaomon", "Git", "fsnotifier",
        "manager", "launcher", "daemon", "system", "proxy", "kde"
    )
    var blacklisted: SnapshotStateList<Process> = mutableStateListOf()
    var hiddenProcesses: MutableList<Process> = resetHiddenProcesses()

    private var allProcesses =
        ProcessHandle.allProcesses().asSequence().associate { handle ->
            (handle.info().command().orElse("")) to RunningProcess(handle)
        }.toMutableMap()


    @JvmOverloads
    fun computeReducedProcessList(
        user: String = System.getProperty("user.name"),
        cmdBlacklist: List<Process> = hiddenProcesses,
    ): Stream<RunningProcess>? {
        ProcessHandle.allProcesses().parallel().forEach { handle ->
            allProcesses.computeIfAbsent(handle.info().command().orElse("")) {
                RunningProcess(handle)
            }
        }
        synchronized(cmdBlacklist) {
            return allProcesses.values
                .parallelStream()
                .distinct()
                .filter { proc ->
                    proc.user.toLowerCase().contains(user.toLowerCase())
                }
                .filter { proc ->
                    cmdBlacklist.none { otherProcess ->
                        proc.equals(otherProcess) || proc.command.contains(
                            otherProcess.stringRepresentation,
                            ignoreCase = true
                        )
                    }
                }
        }
    }

    fun computeFilteredProcessList(): List<RunningProcess> {
        return computeReducedProcessList()
            ?.filter { proc: Process ->
                !blacklisted.contains(
                    proc.command
                )
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
