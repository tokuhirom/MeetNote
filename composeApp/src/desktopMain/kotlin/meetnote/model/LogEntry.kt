package meetnote.model

import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

data class LogEntry(val path: Path) {
    private var _contentCache: String? = null
    val content: String
        get() = _contentCache ?: reloadContent()

    fun reloadContent(): String {
        _contentCache = path.readText()
        return _contentCache!!
    }

    val vttPath : Path by lazy {
        path.resolveSibling(path.name.replace(".md", ".vtt"))
    }

    val mp3Path : Path by lazy {
        path.resolveSibling(path.name.replace(".md", ".mp3"))
    }

    fun title(): String {
        val inputFileName = path.name

        val datePart = inputFileName.substring(0, 8)  // "20230102"
        val timePart = inputFileName.substring(8, 12)  // "0304"

        val formattedDate = datePart.replaceRange(4, 4, "-").replaceRange(7, 7, "-")
        val formattedTime = timePart.replaceRange(2, 2, ":")

        return "$formattedDate $formattedTime"
    }
}
