package nl.fietsweer.app.ui.screens.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.fietsweer.app.data.RadarImages

class PreparedRadar(val times: List<Long>, val images: List<ImageBitmap>, val motion: List<MotionField>)

// The smoothed radar frames and the motion between them, worked out once per set of
// Buienradar images and kept for as long as the app runs.
object RadarAnimation {

    private val mutableState = MutableStateFlow<PreparedRadar?>(null)
    val state: StateFlow<PreparedRadar?> = mutableState.asStateFlow()

    private val lock = Mutex()

    suspend fun prepare() = lock.withLock {
        val raw = RadarImages.frames()
        if (raw.size < 2) return@withLock
        val times = raw.map { it.timeMs }
        if (mutableState.value?.times == times) return@withLock
        val prepared = withContext(Dispatchers.Default) {
            val smoothed = raw.map { RadarSmoothing.smooth(it.bitmap) }
            PreparedRadar(times, smoothed.map { it.asImageBitmap() }, RadarMotion.between(smoothed))
        }
        mutableState.value = prepared
    }
}
