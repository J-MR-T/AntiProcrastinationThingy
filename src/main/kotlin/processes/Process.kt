package processes

import java.nio.file.FileSystems
import java.util.*
import java.util.regex.MatchResult
import java.util.regex.Pattern

class Process {
    private val handle: ProcessHandle
    private var stringRepresentation: String

    constructor(handle: ProcessHandle) {
        this.handle = handle
        stringRepresentation = determineStringRepresentation()
    }

    constructor(handle: ProcessHandle, stringRepresentation: String) {
        this.handle = handle
        this.stringRepresentation = stringRepresentation
    }

    /**
     * Process from Command String, which is always disallowed.
     *
     * @param command The System command which started the process
     */
    constructor(command: String) : this(ProcessHandleFromString(command))

    fun command(): String {
        return handle.info().command().orElse("")
    }

    fun user(): String {
        return handle.info().user().orElse("")
    }

    fun kill(): Boolean {
        return handle.destroy()
    }

    private fun determineStringRepresentation(): String {
        var sep = FileSystems.getDefault().separator
        if (command().contains("\\")) {
            sep = "\\"
        } else if (command().contains("/")) {
            sep = "/"
        }
        val pathParts = command().split(Regex(Pattern.quote(sep))).toTypedArray()
        if (pathParts.isEmpty() || pathParts[0] == "") return ""
        var returnString = pathParts[pathParts.size - 1]
        if (returnString.toUpperCase() != returnString) {
            returnString = returnString.replace("[A-Z]".toRegex(), " $0").replace("\\. ".toRegex(), ".")
        }
        returnString = returnString.toLowerCase()
            .replace("\\.exe|\\.app|64|32".toRegex(), "")
        var m = Pattern.compile(" [a-z]").matcher(returnString)
        returnString = m.replaceAll { matchResult: MatchResult ->
            matchResult.group().toUpperCase()
        }
            .replace("_".toRegex(), " ")
        if (returnString.isBlank()) return ""
        m = Pattern.compile(".").matcher(returnString)
        return m.replaceFirst { matchResult: MatchResult ->
            matchResult.group().toUpperCase()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Process) return false
        return stringRepresentation == other.stringRepresentation
    }

    override fun hashCode(): Int {
        return Objects.hash(stringRepresentation)
    }

    override fun toString(): String {
        return stringRepresentation
    }

    fun setStringRepresentation(stringRepresentation: String) {
        this.stringRepresentation = stringRepresentation
    }
}