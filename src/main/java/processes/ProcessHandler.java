package processes;

import java.util.*;
import java.util.stream.Collectors;

public class ProcessHandler {
    public static final List<String> DEFAULT_HIDDEN_PROCESSES =
            List.of("System32", "Nvidia", "SystemApps", "wallpaper",
                    "Razer", "Native Instruments", "xboxGam", "Microsoft.ZuneVideo", "Settings", "GWSL",
                    "Keyboard Chattering Fix", "YourPhone", "webhelper", "Driver", "Gaomon", "Git", "fsnotifier");
    public static List<String> hiddenProcesses = new ArrayList<>(DEFAULT_HIDDEN_PROCESSES);
    public static List<Process> reducedProcessList = null;

    public static Set<String> blacklisted = new HashSet<>();

    //TODO this might not work with lastAllProcess.equals(), because the ordering of ProcessHandle.allProcesses() is
    // unclear
    private static List<ProcessHandle> lastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());

    public static List<Process> computeReducedProcessList(String user, List<String> cmdBlacklist, boolean force) {
        final List<ProcessHandle> newerLastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());
        if (!force && reducedProcessList != null && lastAllProcesses.equals(newerLastAllProcesses)) {
            return reducedProcessList;
        }
        try {
            lastAllProcesses = newerLastAllProcesses;
            reducedProcessList = lastAllProcesses.stream()
                    .map(Process::new)
                    .distinct()
                    .filter(proc -> proc.user().toLowerCase().contains(user.toLowerCase()))
                    .filter(proc -> cmdBlacklist.stream().map(String::toLowerCase)
                            .noneMatch(item -> proc.command().toLowerCase().contains(item)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            //FIXME: Sometimes:tm:, a concurrent modification Exception occurs here, if the cmdBlacklist is being
            // modified while its stream is being read. This is probably fixable by either locking it with a
            // synchronized block, using a blocking collection, or making a local shallow copy, the elements of which
            // won't be concurrently modified by the user.
            // The workaround, which is much easier, is just returning an empty list for a second, which will
            // be replaced <1 second afterwards
            return Collections.emptyList();
        }
        return reducedProcessList;
    }

    public static List<Process> computeFilteredProcessList(boolean force) {
        return ProcessHandler.computeReducedProcessList(force).stream()
                .filter(proc -> !ProcessHandler.blacklisted.contains(proc.command())).collect(Collectors.toList());
    }

    public static List<Process> computeReducedProcessList(String user) {
        return computeReducedProcessList(user, hiddenProcesses, false);
    }

    public static List<Process> computeReducedProcessList() {
        return computeReducedProcessList(System.getProperty("user.name"), hiddenProcesses, false);
    }

    public static List<Process> computeReducedProcessList(boolean force) {
        return computeReducedProcessList(System.getProperty("user.name"), hiddenProcesses, force);
    }

    public static List<Process> computeReducedProcessList(List<String> cmdBlacklist) {
        return computeReducedProcessList(System.getProperty("user.name"), cmdBlacklist, false);
    }

    public static List<Process> getDisallowedProcessesThatAreRunning() {
        return computeReducedProcessList(true).stream()
                .filter(process -> blacklisted.contains(process.command()))
                .collect(Collectors.toList());
    }

    public static Collection<? extends Process> blacklistedProcesses() {
        if (blacklisted == null) {
            return Collections.emptySet();
        }
        return blacklisted.stream().map(Process::new).collect(Collectors.toList());
    }

    public static void resetHiddenProcesses() {
        hiddenProcesses = new ArrayList<>(DEFAULT_HIDDEN_PROCESSES);
    }
}
