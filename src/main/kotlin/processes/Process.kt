package processes

import io.serialization.ProcessSerializer
import kotlinx.serialization.Serializable
import java.nio.file.FileSystems
import java.util.*
import java.util.regex.MatchResult
import java.util.regex.Pattern

@Serializable(ProcessSerializer::class)
abstract class Process(
    val command: String = "",
    val stringRepresentation: String = prettyProcessString(command),
) {


    private fun determineStringRepresentation(): String {
        return prettyProcessString(this)
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

    companion object {
        fun prettyProcessString(command: String): String {
            if (command.isBlank()) return "";
            var sep = FileSystems.getDefault().separator
            if (command.contains("\\")) {
                sep = "\\"
            } else if (command.contains("/")) {
                sep = "/"
            }

            val pathParts = command.split(Regex(Pattern.quote(sep))).toTypedArray()
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
            }.replace("_".toRegex(), " ")
            if (returnString.isBlank()) return ""
            m = Pattern.compile(".").matcher(returnString)
            return m.replaceFirst { matchResult: MatchResult ->
                matchResult.group().toUpperCase()
            }
        }

        fun prettyProcessString(process: Process): String {
            return prettyProcessString(process.command)
        }
    }

}