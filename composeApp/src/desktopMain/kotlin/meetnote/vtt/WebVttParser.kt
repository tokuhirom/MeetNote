package meetnote.vtt

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Subtitle(val start: LocalTime, val end: LocalTime, val content: String)

fun compactionWebVtt(src: List<Subtitle>): List<Subtitle> {
    val compacted = mutableListOf<Subtitle>()
    var lastSubtitle: Subtitle? = null

    src.forEach { subtitle ->
        if (lastSubtitle == null) {
            lastSubtitle = subtitle
        } else if (lastSubtitle!!.content == subtitle.content) {
            // extend the end time of the last subtitle
            lastSubtitle = Subtitle(lastSubtitle!!.start, subtitle.end, subtitle.content)
        } else {
            // current subtitle is different from last subtitle, so add last subtitle to list
            compacted.add(lastSubtitle!!)
            lastSubtitle = subtitle
        }
    }

    // add last subtitle if not added yet
    if (lastSubtitle != null && (compacted.isEmpty() || compacted.last().end != lastSubtitle!!.end)) {
        compacted.add(lastSubtitle!!)
    }

    return compacted
}

fun parseWebVtt(webVtt: String): List<Subtitle> {
    val lines = webVtt.split("\n")
    if (lines[0] != "WEBVTT") {
        throw RuntimeException("Invalid WebVTT format")
    }

    val timestampRegex = """\d\d:\d\d:\d\d.\d\d\d --> \d\d:\d\d:\d\d.\d\d\d""".toRegex()

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    var start: LocalTime? = null
    var end: LocalTime? = null
    var content = ""

    val subtitles = mutableListOf<Subtitle>()
    fun add() {
        if (start != null) {
            subtitles.add(Subtitle(start!!, end!!, content.trim()))
            content = ""
            start = null
        }
    }

    lines.drop(1).forEach { line ->
        when {
            timestampRegex.matches(line) -> {
                add()

                val times = line.split(" --> ")
                start = LocalTime.parse(times[0], timeFormatter)
                end = LocalTime.parse(times[1], timeFormatter)
            }

            else -> content += "$line\n"
        }
    }
    add()
    return subtitles
}
