package io;

import com.google.gson.Gson;
import javafx.scene.control.Slider;
import processes.ProcessHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GsonHelper {
    private static final Path defaultBlacklistSetPath = Path.of("rsc", "blacklisted.json");
    private static final Path defaultVolumePath = Path.of("rsc", "volume.txt");
    private static final Path defaultHiddenProcessesPath = Path.of("rsc", "hidden.json");
    private static final Gson gson = new Gson();

    public static Set<String> readBlacklistSet(Path path) throws FileNotFoundException {
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), Set.class);
    }

    public static Set<String> readBlacklistSet() throws FileNotFoundException {
        return readBlacklistSet(defaultBlacklistSetPath);
    }

    public static void writeBlacklistSet(Set<String> set, Path path) throws IOException {
        Files.writeString(path, (gson.toJson(set)));
    }

    public static void writeBlacklistSet(Set<String> set) throws IOException {
        writeBlacklistSet(set, defaultBlacklistSetPath);
    }

    public static void writeVolume(double volume, Path path) throws IOException {
        Files.writeString(path, String.valueOf(volume));
    }

    public static void writeVolume(double volume) throws IOException {
        writeVolume(volume, defaultVolumePath);
    }

    public static void writeHiddenProcesses(List<String> hiddenProcesses, Path path) throws IOException {
        Files.writeString(path, gson.toJson(hiddenProcesses));
    }

    public static void writeHiddenProcesses(List<String> hiddenProcesses) throws IOException {
        writeHiddenProcesses(hiddenProcesses, defaultHiddenProcessesPath);
    }

    public static List<String> readHiddenProcesses() throws IOException {
        return readHiddenProcesses(defaultHiddenProcessesPath);
    }

    public static List<String> readHiddenProcesses(Path path) throws IOException {
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), List.class);
    }

    public static void readVolume(Slider volumeSlider) throws IOException {
        readVolume(volumeSlider, defaultVolumePath);
    }

    public static void readVolume(Slider volumeSlider, Path path) throws IOException {
        volumeSlider.setValue(Double.parseDouble(Files.readString(path)));
    }

    public static void startApp(Slider volumeSlider) throws IOException {
        final Set<String> blacklisted = readBlacklistSet();
        ProcessHandler.blacklisted = blacklisted != null ? blacklisted : new HashSet<>();
        final List<String> hiddenProcesses = readHiddenProcesses();
        ProcessHandler.hiddenProcesses =
                hiddenProcesses != null ? hiddenProcesses : new ArrayList<>(ProcessHandler.DEFAULT_HIDDEN_PROCESSES);
        readVolume(volumeSlider);
        ProcessHandler.computeReducedProcessList(true);
    }

    public static void stopApp(double volume) throws IOException {
        writeBlacklistSet(ProcessHandler.blacklisted);
        writeVolume(volume);
        writeHiddenProcesses(ProcessHandler.hiddenProcesses);
    }
}
