package processes;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Process {
    private final ProcessHandle handle;
    @NotNull
    private String stringRepresentation;

    public Process(ProcessHandle handle) {
        this.handle = handle;
        this.stringRepresentation = determineStringRepresentation();
    }

    public Process(ProcessHandle handle, String stringRepresentation) {
        this.handle = handle;
        this.stringRepresentation = stringRepresentation;
    }

    /**
     * Process from Command String, which is always disallowed.
     *
     * @param command The System command which started the process
     */
    public Process(String command) {
        this(new ProcessHandleFromString(command));
    }

    public String command() {
        return handle.info().command().orElse("");
    }

    public String user() {
        return handle.info().user().orElse("");
    }

    public boolean kill() {
        return handle.destroy();
    }

    private String determineStringRepresentation() {
        String[] pathParts = command().split(Pattern.quote("\\"));
        if (pathParts.length == 0) return "";
        String returnString = pathParts[pathParts.length - 1].toLowerCase().replaceAll(".exe|64|32", "");
        if (returnString.isBlank()) return "";
        return Character.toUpperCase(returnString.charAt(0)) + returnString.substring(1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Process)) return false;
        Process process = (Process) o;
        return this.stringRepresentation.equals(process.stringRepresentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stringRepresentation);
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

    public void setStringRepresentation(@NotNull String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }
}
