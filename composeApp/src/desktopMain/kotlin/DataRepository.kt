import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
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

        return dataDirectory.toFile().listFiles()?.filter {
            it.isFile && it.name.endsWith(".wav")
        }?.map {
            it.toPath()
        } ?: emptyList()
    }

    private fun getDataDirectory() : Path {
        return Paths.get(System.getProperty("user.home")).resolve("MeetNote")
    }

    fun getRecentSummarizedFiles(): List<Path> {
        val dataDirectory = getDataDirectory()
        if (!dataDirectory.toFile().exists()) {
            return emptyList()
        }

        return dataDirectory.toFile().listFiles()?.filter {
            it.isFile && it.name.endsWith(".md")
        }?.map {
            it.toPath()
        }?.sortedByDescending { it.name } ?: emptyList()
    }
}