package processes;

import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String sep = FileSystems.getDefault().getSeparator();
        if (command().contains("\\")) {
            sep = "\\";
        } else if (command().contains("/")) {
            sep = "/";
        }
        String[] pathParts = command().split(Pattern.quote(sep));
        if (pathParts.length == 0) return "";
        String returnString = pathParts[pathParts.length - 1];
        if(!returnString.toUpperCase().equals(returnString)){
            returnString=returnString.replaceAll("[A-Z]", " $0").replaceAll("\\. ",".");
        }
        returnString = returnString
                .toLowerCase()
                .replaceAll("\\.exe|\\.app|64|32", "");
        Matcher m = Pattern.compile(" [a-z]").matcher(returnString);
        returnString = m.replaceAll(matchResult -> matchResult.group().toUpperCase())
                .replaceAll("_", " ");
        if (returnString.isBlank()) return "";
        m = Pattern.compile(".").matcher(returnString);
        return m.replaceFirst(matchResult -> matchResult.group().toUpperCase());
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
