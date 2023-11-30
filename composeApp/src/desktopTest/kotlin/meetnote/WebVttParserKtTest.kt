package meetnote


import assertk.assertThat
import assertk.assertions.isEqualTo
import meetnote.vtt.Subtitle
import meetnote.vtt.compactionWebVtt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class WebVttParserKtTest {

    @Test
    fun parseWebVtt() {
        val inputStr = """
            WEBVTT

            00:00:30.000 --> 00:00:32.000
            お待たせしました。

            00:01:00.000 --> 00:01:02.000
            なるほどですね。
        """.trimIndent()

        val result = meetnote.vtt.parseWebVtt(inputStr)

        assertEquals(2, result.size)

        assertThat(result[0].start).isEqualTo(LocalTime.of(0, 0, 30, 0))
        assertThat(result[0].end).isEqualTo(LocalTime.of(0, 0, 32, 0))
        assertThat(result[0].content).isEqualTo("お待たせしました。")

        assertThat(result[1].start).isEqualTo(LocalTime.of(0, 1, 0, 0))
        assertThat(result[1].end).isEqualTo(LocalTime.of(0, 1, 2, 0))
        assertThat(result[1].content).isEqualTo("なるほどですね。")
    }


    @Test
    fun testCompactionWebVtt() {
        val inputList = listOf(
            Subtitle(LocalTime.of(0, 0, 30, 0), LocalTime.of(0, 0, 32, 0), "お待たせしました。"),
            Subtitle(LocalTime.of(0, 1, 0, 0), LocalTime.of(0, 1, 2, 0), "お待たせしました。"),
            Subtitle(LocalTime.of(0, 5, 0, 0), LocalTime.of(0, 5, 2, 0), "1点目の説明です。"),
            Subtitle(LocalTime.of(0, 5, 2, 0), LocalTime.of(0, 5, 5, 0), "1点目の説明です。"),
            Subtitle(LocalTime.of(0, 5, 5, 0), LocalTime.of(0, 5, 7, 0), "2点目の説明です。")
        )
        val result = compactionWebVtt(inputList)

        assertThat(result.size).isEqualTo(3)

        assertThat(result[0].start).isEqualTo(LocalTime.of(0, 0, 30, 0))
        assertThat(result[0].end).isEqualTo(LocalTime.of(0, 1, 2, 0))
        assertThat(result[0].content).isEqualTo("お待たせしました。")

        assertThat(result[1].start).isEqualTo(LocalTime.of(0, 5, 0, 0))
        assertThat(result[1].end).isEqualTo(LocalTime.of(0, 5, 5, 0))
        assertThat(result[1].content).isEqualTo("1点目の説明です。")

        assertThat(result[2].start).isEqualTo(LocalTime.of(0, 5, 5, 0))
        assertThat(result[2].end).isEqualTo(LocalTime.of(0, 5, 7, 0))
        assertThat(result[2].content).isEqualTo("2点目の説明です。")
    }
}
