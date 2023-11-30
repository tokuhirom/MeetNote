package meetnote

import meetnote.model.LogEntry
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class DataRepository {
    fun getNewWaveFilePath(): Path {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd/yyyyMMddHHmmss")
        val formattedDateTime = currentDateTime.format(formatter)
        val path = getDataDirectory().resolve("$formattedDateTime.wav")
        path.parent.toFile().mkdirs()
        return path
    }

    fun getUnprocessedWaveFiles(): List<Path> {
        val dataDirectory = getDataDirectory()
        if (!dataDirectory.toFile().exists()) {
            return emptyList()
        }

        return Files.walk(dataDirectory)
            .filter {
                it.isRegularFile() && it.fileName.toString().endsWith(".wav")
            }
            .toList()
    }

    fun getDataDirectory() : Path {
        return Paths.get(System.getProperty("user.home")).resolve("MeetNote")
    }

    private fun getRecentSummarizedFiles(): List<Path> {
        val dataDirectory = getDataDirectory()
        if (!dataDirectory.toFile().exists()) {
            return emptyList()
        }

        return Files.walk(dataDirectory)
            .filter { it.isRegularFile() && it.name.endsWith(".md") }
            .sorted { p1, p2 -> p2.fileName.compareTo(p1.fileName) }
            .toList()
    }

    fun getRecentSummarizedLogs(): List<LogEntry> {
        return getRecentSummarizedFiles().map { LogEntry(it) }
    }
}
