package processes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Process {
    private final ProcessHandle handle;
    private final String stringRepresentation;

    public Process(ProcessHandle handle) {
        this.handle = handle;
        this.stringRepresentation = determineStringRepresentation();
    }

    /**
     * Process from Command String, which is always disallowed.
     *
     * @param command The System command which started the process
     */
    public Process(String command) {
        this(new ProcessHandle() {
            @Override
            public long pid() {
                return 0;
            }

            @Override
            public Optional<ProcessHandle> parent() {
                return Optional.empty();
            }

            @Override
            public Stream<ProcessHandle> children() {
                return null;
            }

            @Override
            public Stream<ProcessHandle> descendants() {
                return null;
            }

            @Override
            public Info info() {
                return new Info() {
                    @Override
                    public Optional<String> command() {
                        return Optional.of(command);
                    }

                    @Override
                    public Optional<String> commandLine() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String[]> arguments() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Instant> startInstant() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Duration> totalCpuDuration() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> user() {
                        return Optional.of("apoc");
                    }
                };
            }

            @Override
            public CompletableFuture<ProcessHandle> onExit() {
                return null;
            }

            @Override
            public boolean supportsNormalTermination() {
                return false;
            }

            @Override
            public boolean destroy() {
                return false;
            }

            @Override
            public boolean destroyForcibly() {
                return false;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public int hashCode() {
                return Objects.hash(info().command(), info().user());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof ProcessHandle)) return false;
                ProcessHandle process = (ProcessHandle) other;
                return info().command().orElse("").equals(process.info().command().orElse("")) &&
                        info().user().orElse("").equals(process.info().user().orElse(""));
            }

            @Override
            public int compareTo(ProcessHandle other) {
                int dontLetItBeEqual = this.info().command().orElse("").compareTo(other.info().command().orElse(""))
                        + this.info().user().orElse("").compareTo(other.info().user().orElse(""));
                if(dontLetItBeEqual==0&&!this.equals(other)) return -1;
                return dontLetItBeEqual;
            }
        });
    }

    public String command() {
        return handle.info().command().orElse("");
    }

    public String user() {
        return handle.info().user().orElse("");
    }

    public boolean kill(){
        return handle.destroy();
    }

    private String determineStringRepresentation() {
        String[] pathParts = command().split(Pattern.quote("\\"));
        if (pathParts.length == 0) return "";
        String returnString = pathParts[pathParts.length - 1].replaceAll(".exe|64|32", "");
        if ("".equals(returnString)) return "";
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

}
