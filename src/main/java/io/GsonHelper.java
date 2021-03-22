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

public class GsonHelper {
    private static final Path defaultBlacklistSetPath = Path.of("rsc", "blacklisted.json");
    private static final Path defaultVolumePath= Path.of("rsc", "volume.txt");
    private static final Gson gson= new Gson();

    public static CommandSet readBlacklistSet(Path path) throws FileNotFoundException {
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), CommandSet.class);
    }

    public static CommandSet readBlacklistSet() throws FileNotFoundException {
        return readBlacklistSet(defaultBlacklistSetPath);
    }

    public static void writeBlacklistSet(CommandSet set, Path path) throws IOException {
        Files.writeString(path, (gson.toJson(set)));
    }

    public static void writeBlacklistSet(CommandSet set) throws IOException {
        writeBlacklistSet(set, defaultBlacklistSetPath);
    }

    public static void writeVolume(double volume,Path path) throws IOException{
        Files.writeString(path,String.valueOf(volume));
    }

    public static void writeVolume(double volume) throws IOException{
        writeVolume(volume,defaultVolumePath);
    }

    public static void startApp(Slider volumeSlider) throws IOException{
        final CommandSet blacklisted = readBlacklistSet();
        ProcessHandler.blacklisted= blacklisted!=null?blacklisted:new CommandSet();
        readVolume(volumeSlider);
    }

    public static void readVolume(Slider volumeSlider) throws IOException {
        readVolume(volumeSlider,defaultVolumePath);
    }

    public static void readVolume(Slider volumeSlider,Path path) throws IOException {
        volumeSlider.setValue(Double.parseDouble(Files.readString(path)));
    }

    public static void stopApp(double volume) throws IOException {
        writeBlacklistSet(ProcessHandler.blacklisted);
        writeVolume(volume);
    }
}
