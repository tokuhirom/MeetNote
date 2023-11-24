package meetnote

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class ConfigRepository {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    // 設定を保存する
    fun saveConfiguration(config: Config, path: Path = configPath()) {
        val jsonData = objectMapper.writeValueAsString(config)

        if (path.parent.notExists()) {
            path.parent.toFile().mkdirs()
        }

        logger.info("Saving $path: $jsonData")
        val tempFile = path.resolveSibling(path.name + ".tmp")
        tempFile.writeText(jsonData)
        Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    // 設定を読み込む
    fun loadSettings(path: Path = configPath()): Config {
        if (!path.exists()) return Config()
        val jsonData = path.toFile().readText()
        return objectMapper.readValue(jsonData)
    }

    private fun configPath(): Path {
        val settingsPath = System.getProperty("user.home") + "/.config/meetnote/config.json"
        return Paths.get(settingsPath)
    }
}
