package meetnote

import assertk.assertThat
import assertk.assertions.isEqualTo
import meetnote.WindowNameCollector.WindowState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class WindowNameCollectorTest {

    @EnabledOnOs(OS.MAC)
    @Test
    fun testGetWindowListString() {
        val res = WindowNameCollector().getWindowListString()
        println(res)
    }

    @Test
    fun testParseWindowState() {
        val res = WindowNameCollector().parseWindowState(
            """
                <BUNDLEID>us.zoom.xos</BUNDLEID><WINDOW></WINDOW>
                <BUNDLEID>us.zoom.xos</BUNDLEID><WINDOW>Zoom Meeting</WINDOW>
                <BUNDLEID>us.zoom.xos</BUNDLEID><WINDOW>Zoom</WINDOW>
            """.trimIndent()
        )

        assertThat(res).isEqualTo(listOf(
            WindowState(
                bundleId = "us.zoom.xos",
                windowName = ""
            ),
            WindowState(
                bundleId = "us.zoom.xos",
                windowName = "Zoom Meeting"
            ),
            WindowState(
                bundleId = "us.zoom.xos",
                windowName = "Zoom"
            )
        ))
    }
}
