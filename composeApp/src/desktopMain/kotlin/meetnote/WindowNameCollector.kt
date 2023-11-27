package meetnote

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class WindowNameCollector {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class WindowState(val bundleId: String, val windowName: String)

    fun getWindowStateList(): List<WindowState> {
        val windowListString = getWindowListString()
        logger.info("windowListString: $windowListString")

        return parseWindowState(windowListString)
    }

    internal fun parseWindowState(input: String): List<WindowState> {
        // Regex pattern for matching the input string.
        val pattern = """<BUNDLEID>(.*?)</BUNDLEID><WINDOW>(.*?)</WINDOW>""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Try to find a match in the input string.
        val matchResults = pattern.findAll(input)
        return matchResults.map { matchResult ->
            val (bundleId, windowName) = matchResult.destructured
            WindowState(bundleId, windowName)
        }.toList()
    }

    internal fun getWindowListString(): String {
        val script = """
tell application "System Events"
    set procs to processes
    set results to ""
    repeat with proc in procs
        if exists (window 1 of proc) then
            repeat with w in windows of proc
                set results to results & "<BUNDLEID>" & bundle identifier of proc & "</BUNDLEID><WINDOW>" & name of w & "</WINDOW>\n"
            end repeat
        end if
    end repeat
    return results
end tell
        """.trimIndent()

        val pb = ProcessBuilder("osascript", "-e", script)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val p = pb.start()

        val lines = BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
            reader.readText()
        }

        val exitStatus = p.waitFor() // Wait for the process to finish.
        if (exitStatus != 0) {
            throw RuntimeException("Failed to execute osascript. Exit status: $exitStatus, lines='$lines'")
        }

        return lines
    }
}
