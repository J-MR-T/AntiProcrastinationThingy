package processes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

class ProcessHandleFromString implements ProcessHandle {
    private final String command;
    private final String user;

    public ProcessHandleFromString(String command, String user) {
        this.command = command;
        this.user = user;
    }

    public ProcessHandleFromString(String command) {
        this(command,"apoc");
    }


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
    public ProcessHandle.Info info() {
        return new ProcessHandle.Info() {
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
                return Optional.of(user);
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
        if (dontLetItBeEqual == 0 && !this.equals(other)) return -1;
        return dontLetItBeEqual;
    }
}
