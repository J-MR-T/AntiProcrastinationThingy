package io;

import com.google.gson.Gson;
import processes.ProcessHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class GsonHelper {
    public static Path defaultPath = Path.of("rsc", "blacklisted.json");

    public static CommandSet readBlacklistSet(Path path) throws FileNotFoundException {
        Gson gson = new Gson();
        return gson.fromJson(new InputStreamReader(new FileInputStream(path.toFile())), CommandSet.class);
    }

    public static CommandSet readBlacklistSet() throws FileNotFoundException {
        return readBlacklistSet(defaultPath);
    }

    public static void writeBlacklistSet(CommandSet set, Path path) throws IOException {
        Gson gson = new Gson();
        Files.writeString(path, (gson.toJson(set)));
    }

    public static void writeBlacklistSet(CommandSet set) throws IOException {
        writeBlacklistSet(set, defaultPath);
    }

    public static void startApp() throws IOException{
        final CommandSet blacklisted = readBlacklistSet();
        ProcessHandler.blacklisted= blacklisted!=null?blacklisted:new CommandSet();
    }

    public static void stopApp() throws IOException {
        writeBlacklistSet(ProcessHandler.blacklisted);
    }
}
