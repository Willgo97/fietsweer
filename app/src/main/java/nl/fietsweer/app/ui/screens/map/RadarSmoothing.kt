package nl.fietsweer.app.ui.screens.map

import android.graphics.Bitmap

// Radar pixels are about a kilometre each; three box blurs approximate a gaussian so the
// showers get soft round edges instead of blocks. Done on premultiplied colour so the
// transparent background does not darken the edges.
object RadarSmoothing {

    private const val RADIUS = 1
    private const val PASSES = 3

    fun smooth(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val channels = Array(4) { FloatArray(pixels.size) }
        for (i in pixels.indices) {
            val p = pixels[i]
            val alpha = (p ushr 24) / 255f
            channels[0][i] = alpha
            channels[1][i] = ((p shr 16) and 0xFF) / 255f * alpha
            channels[2][i] = ((p shr 8) and 0xFF) / 255f * alpha
            channels[3][i] = (p and 0xFF) / 255f * alpha
        }
        val scratch = FloatArray(pixels.size)
        for (channel in channels) repeat(PASSES) {
            boxBlur(channel, scratch, width, height, horizontal = true)
            boxBlur(scratch, channel, width, height, horizontal = false)
        }
        for (i in pixels.indices) {
            val alpha = channels[0][i].coerceIn(0f, 1f)
            if (alpha < 1f / 255f) { pixels[i] = 0; continue }
            fun colour(c: Float) = (c / alpha * 255f).toInt().coerceIn(0, 255)
            pixels[i] = ((alpha * 255f).toInt() shl 24) or (colour(channels[1][i]) shl 16) or
                (colour(channels[2][i]) shl 8) or colour(channels[3][i])
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(input: FloatArray, output: FloatArray, width: Int, height: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val window = 2 * RADIUS + 1
        for (line in 0 until lines) {
            fun index(position: Int): Int {
                val clamped = position.coerceIn(0, length - 1)
                return if (horizontal) line * width + clamped else clamped * width + line
            }
            var sum = 0f
            for (k in -RADIUS..RADIUS) sum += input[index(k)]
            for (position in 0 until length) {
                output[index(position)] = sum / window
                sum += input[index(position + RADIUS + 1)] - input[index(position - RADIUS)]
            }
        }
    }
}
