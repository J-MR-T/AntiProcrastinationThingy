package processes

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream

internal class ProcessHandleFromString @JvmOverloads constructor(
    private val command: String,
    private val user: String = "apoc"
) :
    ProcessHandle {
    override fun pid(): Long {
        return 0
    }

    override fun parent(): Optional<ProcessHandle> {
        return Optional.empty()
    }

    override fun children(): Stream<ProcessHandle> {
        return Stream.empty()
    }

    override fun descendants(): Stream<ProcessHandle> {
        return Stream.empty()
    }

    override fun info(): ProcessHandle.Info {
        return object : ProcessHandle.Info {
            override fun command(): Optional<String> {
                return Optional.of(command)
            }

            override fun commandLine(): Optional<String> {
                return Optional.empty()
            }

            override fun arguments(): Optional<Array<String>> {
                return Optional.empty()
            }

            override fun startInstant(): Optional<Instant> {
                return Optional.empty()
            }

            override fun totalCpuDuration(): Optional<Duration> {
                return Optional.empty()
            }

            override fun user(): Optional<String> {
                return Optional.of(user)
            }
        }
    }

    override fun onExit(): CompletableFuture<ProcessHandle> {
        return CompletableFuture()
    }

    override fun supportsNormalTermination(): Boolean {
        return false
    }

    override fun destroy(): Boolean {
        return false
    }

    override fun destroyForcibly(): Boolean {
        return false
    }

    override fun isAlive(): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(info().command(), info().user())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessHandle) return false
        val process = other
        return info().command().orElse("") == process.info().command().orElse("") && info().user()
            .orElse("") == process.info().user().orElse("")
    }

    override fun compareTo(other: ProcessHandle): Int {
        val dontLetItBeEqual = (info().command().orElse("").compareTo(other.info().command().orElse(""))
                + info().user().orElse("").compareTo(other.info().user().orElse("")))
        return if (dontLetItBeEqual == 0 && this != other) -1 else dontLetItBeEqual
    }
}