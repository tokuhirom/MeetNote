package meetnote.audio

import kotlin.math.sqrt

// Calcurate RMS(root mean square) to detect no-sound file.
// if rms is too low, the file may not record anything.
fun audioRms(audioBytes: ByteArray): Double {
    val audioData = audioBytes.map { it.toInt() }

    var squareSum = 0.0
    for (sample in audioData) {
        squareSum += sample * sample
    }
    return sqrt(squareSum / audioData.size)
}
