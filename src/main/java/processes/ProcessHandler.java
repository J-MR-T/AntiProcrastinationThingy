package processes;

import io.CommandSet;

import java.util.*;
import java.util.stream.Collectors;

public class ProcessHandler {
    public static final List<String> DEFAULT_BLACKLIST =
            Arrays.asList("System32", "Nvidia", "SystemApps", "wallpaper",
                    "Razer", "Native Instruments", "xboxGam", "Microsoft.ZuneVideo", "Settings", "GWSL",
                    "Keyboard Chattering Fix", "YourPhone", "webhelper", "Driver","Gaomon","Git");
    public static List<String> currentBlacklist = new ArrayList<>(DEFAULT_BLACKLIST);
    public static List<Process> reducedProcessList = null;

    public static CommandSet blacklisted = new CommandSet();

    //TODO this might not work with lastAllProcess.equals(), because the ordering of ProcessHandle.allProcesses() is
    // unclear
    private static List<ProcessHandle> lastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());

    public static List<Process> computeReducedProcessList(String user, List<String> cmdBlacklist, boolean force) {
        final List<ProcessHandle> newerLastAllProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());
        if (!force && reducedProcessList != null && lastAllProcesses.equals(newerLastAllProcesses)) {
            return reducedProcessList;
        }
        lastAllProcesses = newerLastAllProcesses;
        reducedProcessList = lastAllProcesses.stream()
                .map(Process::new)
                .distinct()
                .filter(proc -> proc.user().toLowerCase().contains(user.toLowerCase()))
                .filter(proc -> cmdBlacklist.stream().map(String::toLowerCase)
                        .noneMatch(item -> proc.command().toLowerCase().contains(item)))
                .collect(Collectors.toList());
        return reducedProcessList;
    }

    public static List<Process> computeReducedProcessList(String user) {
        return computeReducedProcessList(user, currentBlacklist, false);
    }

    public static List<Process> computeReducedProcessList() {
        return computeReducedProcessList(System.getProperty("user.name"), currentBlacklist, false);
    }

    public static List<Process> computeReducedProcessList(boolean force) {
        return computeReducedProcessList(System.getProperty("user.name"), currentBlacklist, force);
    }

    public static List<Process> computeReducedProcessList(List<String> cmdBlacklist) {
        return computeReducedProcessList(System.getProperty("user.name"), cmdBlacklist, false);
    }

    public static List<Process> getDisallowedProcessesThatAreRunning() {
        return computeReducedProcessList().stream()
                .filter(process -> blacklisted.contains(process.command()))
                .collect(Collectors.toList());
    }

    public static Collection<? extends Process> blacklistedProcesses() {
        if (blacklisted == null) {
            return Collections.emptySet();
        }
        return blacklisted.stream().map(Process::new).collect(Collectors.toList());
    }

}
