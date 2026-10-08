package nl.fietsweer.app.ui.screens.map

import android.graphics.Bitmap
import kotlin.math.abs

// Coarse motion field between two radar images, on a mesh of (COLUMNS + 1) × (ROWS + 1) vertices.
// Displacements are in pixels of the original image: where the rain at a vertex in the first
// image has moved to in the second.
class MotionField(val dx: FloatArray, val dy: FloatArray)

object RadarMotion {

    const val COLUMNS = 24
    const val ROWS = 21

    private const val SCALE = 4
    private const val BLOCK = 5
    private const val SEARCH = 5
    private const val MIN_SIGNAL = 400

    fun between(images: List<Bitmap>): List<MotionField> {
        val small = images.map { alphaOf(Bitmap.createScaledBitmap(it, it.width / SCALE, it.height / SCALE, true)) }
        return small.zipWithNext { earlier, later -> motionBetween(earlier, later) }
    }

    private class Plane(val width: Int, val height: Int, val values: IntArray) {
        fun at(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= width || y >= height) 0 else values[y * width + x]
    }

    private fun alphaOf(bitmap: Bitmap): Plane {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return Plane(bitmap.width, bitmap.height, IntArray(pixels.size) { pixels[it] ushr 24 })
    }

    private fun motionBetween(earlier: Plane, later: Plane): MotionField {
        val count = (COLUMNS + 1) * (ROWS + 1)
        val dx = FloatArray(count) { Float.NaN }
        val dy = FloatArray(count) { Float.NaN }
        for (row in 0..ROWS) for (column in 0..COLUMNS) {
            val centreX = column * (earlier.width - 1) / COLUMNS
            val centreY = row * (earlier.height - 1) / ROWS
            var signal = 0
            for (y in -BLOCK..BLOCK) for (x in -BLOCK..BLOCK) signal += earlier.at(centreX + x, centreY + y)
            if (signal < MIN_SIGNAL) continue
            var bestCost = Int.MAX_VALUE
            var bestX = 0
            var bestY = 0
            for (offsetY in -SEARCH..SEARCH) for (offsetX in -SEARCH..SEARCH) {
                var cost = 0
                for (y in -BLOCK..BLOCK) {
                    for (x in -BLOCK..BLOCK) cost += abs(earlier.at(centreX + x, centreY + y) - later.at(centreX + x + offsetX, centreY + y + offsetY))
                    if (cost >= bestCost) break
                }
                if (cost < bestCost || (cost == bestCost && offsetX * offsetX + offsetY * offsetY < bestX * bestX + bestY * bestY)) {
                    bestCost = cost; bestX = offsetX; bestY = offsetY
                }
            }
            val vertex = row * (COLUMNS + 1) + column
            dx[vertex] = (bestX * SCALE).toFloat()
            dy[vertex] = (bestY * SCALE).toFloat()
        }
        fill(dx); fill(dy)
        repeat(3) { smooth(dx); smooth(dy) }
        return MotionField(dx, dy)
    }

    // Empty areas have no motion of their own; give them the median of what does move.
    private fun fill(values: FloatArray) {
        val known = values.filter { !it.isNaN() }.sorted()
        val median = if (known.isEmpty()) 0f else known[known.size / 2]
        for (i in values.indices) if (values[i].isNaN()) values[i] = median
    }

    private fun smooth(values: FloatArray) {
        val copy = values.copyOf()
        for (row in 0..ROWS) for (column in 0..COLUMNS) {
            var sum = 0f
            var count = 0
            for (neighbourRow in maxOf(0, row - 1)..minOf(ROWS, row + 1)) {
                for (neighbourColumn in maxOf(0, column - 1)..minOf(COLUMNS, column + 1)) {
                    sum += copy[neighbourRow * (COLUMNS + 1) + neighbourColumn]; count++
                }
            }
            values[row * (COLUMNS + 1) + column] = sum / count
        }
    }
}
