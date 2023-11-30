package meetnote

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists

fun deleteFileWithSameNameVtt(file: File) {
    val vttFilePath = file.absolutePath.replace(".md", ".vtt")
    val vttFile = Paths.get(vttFilePath)
    vttFile.deleteIfExists()
    file.delete()
}

