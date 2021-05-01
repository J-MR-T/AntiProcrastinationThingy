package processes

import java.util.stream.Collectors

object ProcessHandler {
    val DEFAULT_HIDDEN_PROCESSES = java.util.List.of(
        "System32", "Nvidia", "SystemApps", "wallpaper",
        "Razer", "Native Instruments", "xboxGam", "Microsoft.ZuneVideo", "Settings", "GWSL",
        "Keyboard Chattering Fix", "YourPhone", "webhelper", "Driver", "Gaomon", "Git", "fsnotifier",
        "manager", "launcher", "daemon", "system", "proxy", "kde"
    )
    var hiddenProcesses: MutableList<String> = ArrayList(DEFAULT_HIDDEN_PROCESSES)
    var reducedProcessList: MutableList<Process>? = null
    var blacklisted: MutableSet<String> = HashSet()

    //TODO this might not work with lastAllProcess.equals(), because the ordering of ProcessHandle.allProcesses() is
    // unclear
    private var lastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList())

    @JvmOverloads
    fun computeReducedProcessList(
        force: Boolean = false,
        user: String = System.getProperty("user.name"),
        cmdBlacklist: List<String> = hiddenProcesses,
    ): List<Process>? {
        val newerLastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList())
        if (!force && reducedProcessList != null && lastAllProcesses == newerLastAllProcesses) {
            return reducedProcessList
        }
        try {
            lastAllProcesses = newerLastAllProcesses
            synchronized(cmdBlacklist) {
                reducedProcessList = lastAllProcesses.stream()
                    .map { handle: ProcessHandle ->
                        Process(
                            handle
                        )
                    }
                    .distinct()
                    .filter { proc: Process ->
                        proc.user().toLowerCase().contains(user.toLowerCase())
                    }
                    .filter { proc: Process ->
                        cmdBlacklist.stream().map { obj: String -> obj.toLowerCase() }
                            .noneMatch { item: String? ->
                                proc.command().toLowerCase().contains(
                                    item!!
                                )
                            }
                    }
                    .collect(Collectors.toList())
            }
        } catch (e: Exception) {
            //FIXME: Sometimes:tm:, a concurrent modification Exception occurs here, if the cmdBlacklist is being
            // modified while its stream is being read. This is probably fixable by either locking it with a
            // synchronized block, using a blocking collection, or making a local shallow copy, the elements of which
            // won't be concurrently modified by the user.
            // The workaround, which is much easier, is just returning an empty list for a second, which will
            // be replaced <1 second afterwards
            return emptyList()
        }
        return reducedProcessList
    }

    fun computeFilteredProcessList(force: Boolean): List<Process> {
        return computeReducedProcessList(force = force)?.stream()
            ?.filter { proc: Process ->
                !blacklisted.contains(
                    proc.command()
                )
            }?.collect(Collectors.toList()) ?: emptyList()
    }

    val disallowedProcessesThatAreRunning: List<Process>
        get() {
            synchronized(blacklisted) {
                return computeReducedProcessList(force = true)!!
                    .stream()
                    .filter { process: Process ->
                        blacklisted.contains(
                            process.command()
                        )
                    }
                    .collect(Collectors.toList())
            }
        }

    val blacklistedProcesses: Collection<Process>
        get() {
            synchronized(blacklisted) {
                return blacklisted.stream().map { command: String? -> Process(command!!) }
                    .collect(Collectors.toList())
            }
        }

    fun resetHiddenProcesses() {
        hiddenProcesses = ArrayList(DEFAULT_HIDDEN_PROCESSES)
    }
}
