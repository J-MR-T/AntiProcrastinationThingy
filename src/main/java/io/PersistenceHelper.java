package io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import javafx.scene.control.Slider;
import processes.Process;
import processes.ProcessHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersistenceHelper {
    private static final Path DEFAULT_BLACKLIST_SET_PATH = Path.of("res", "blacklisted.json");
    private static final Path DEFAULT_VOLUME_PATH = Path.of("res", "volume.txt");
    private static final Path DEFAULT_HIDDEN_PROCESSES_PATH = Path.of("res", "hidden.json");
    private static final Gson gson = new Gson();

    public static Set<String> readBlacklistSet(Path path) throws FileNotFoundException {
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), Set.class);
    }

    public static Set<String> readBlacklistSet() throws FileNotFoundException {
        return readBlacklistSet(DEFAULT_BLACKLIST_SET_PATH);
    }

    public static void writeBlacklistSet(Set<String> set, Path path) throws IOException {
        Files.writeString(path, (gson.toJson(set)));
    }

    public static void writeBlacklistSet(Set<String> set) throws IOException {
        writeBlacklistSet(set, DEFAULT_BLACKLIST_SET_PATH);
    }

    public static void writeVolume(double volume, Path path) throws IOException {
        Files.writeString(path, String.valueOf(volume * 100));
    }

    public static void writeVolume(double volume) throws IOException {
        writeVolume(volume, DEFAULT_VOLUME_PATH);
    }

    public static void writeHiddenProcesses(List<String> hiddenProcesses, Path path) throws IOException {
        Files.writeString(path, gson.toJson(hiddenProcesses));
    }

    public static void writeHiddenProcesses(List<String> hiddenProcesses) throws IOException {
        writeHiddenProcesses(hiddenProcesses, DEFAULT_HIDDEN_PROCESSES_PATH);
    }

    public static List<String> readHiddenProcesses() throws IOException {
        return readHiddenProcesses(DEFAULT_HIDDEN_PROCESSES_PATH);
    }

    public static List<String> readHiddenProcesses(Path path) throws IOException {
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), List.class);
    }

    public static void readVolume(Slider volumeSlider) throws IOException {
        readVolume(volumeSlider, DEFAULT_VOLUME_PATH);
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
        if (volumeSlider != null) {
            readVolume(volumeSlider);
        }
        ProcessHandler.computeReducedProcessList(true);
    }

    public static void stopApp(Double volume) throws IOException {
        writeBlacklistSet(ProcessHandler.blacklisted);
        if (volume != null) {
            writeVolume(volume);
        }
        writeHiddenProcesses(ProcessHandler.hiddenProcesses);
    }

    public static void startApp() throws IOException {
        startApp(null);
    }

    public static void stopApp() throws IOException {
        stopApp(null);
    }

    public static <T> void saveObjectToFile(T object, Path path) throws IOException {
        Files.writeString(path, gson.toJson(object));
    }

    public static <T> T readObjectFromFile(Class<T> tClass, Path path) throws IOException {
        return gson.fromJson(Files.readString(path), tClass);
    }
}
