package meetnote

import assertk.assertThat
import assertk.assertions.isEqualTo
import meetnote.WindowNameCollector.WindowState
import org.junit.jupiter.api.Test

class WindowNameCollectorTest {

    @Test
    fun testParseWindowState() {
        val res = WindowNameCollector().parseWindowState(
            """
                Process: 'zoom.us', PID: '788', Bundle ID: 'us.zoom.xos', Window: ''
                Process: 'zoom.us', PID: '788', Bundle ID: 'us.zoom.xos', Window: 'Zoom Meeting'
                Process: 'zoom.us', PID: '788', Bundle ID: 'us.zoom.xos', Window: 'Zoom'
            """.trimIndent()
        )

        assertThat(res).isEqualTo(listOf(
            WindowState(
                processName = "zoom.us",
                processId = "788",
                bundleId = "us.zoom.xos",
                windowName = ""
            ),
            WindowState(
                processName = "zoom.us",
                processId = "788",
                bundleId = "us.zoom.xos",
                windowName = "Zoom Meeting"
            ),
            WindowState(
                processName = "zoom.us",
                processId = "788",
                bundleId = "us.zoom.xos",
                windowName = "Zoom"
            )
        ))
    }
}
