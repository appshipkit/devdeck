package io.devdeck.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.drawToBitmap
import io.devdeck.logs.DeckLogger
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

public object ScreenCapture {

    public var isRecording: Boolean by mutableStateOf(false)
        internal set

    // Screenshots in progress; while any are, DevDeck's floating button, badges and recording bar stay out of them.
    private var capturesInProgress by mutableIntStateOf(0)

    internal val isCapturing: Boolean get() = capturesInProgress > 0

    internal sealed interface LongScreenshotOutcome {
        class Saved(val file: File) : LongScreenshotOutcome

        data object NothingToScroll : LongScreenshotOutcome

        data object Failed : LongScreenshotOutcome
    }

    internal var isRecordingPaused by mutableStateOf(false)

    // Recording time is the time before the last resume plus the time since it, so pauses don't count.
    internal var recordedMillisBeforeResume by mutableLongStateOf(0L)
    internal var resumedAtMillis by mutableLongStateOf(0L)

    // MediaRecorder.pause() arrived in Android 7.0.
    internal val canPauseRecording: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    internal fun recordingElapsedMillis(now: Long = SystemClock.elapsedRealtime()): Long =
        recordedMillisBeforeResume + if (isRecordingPaused) 0 else (now - resumedAtMillis).coerceAtLeast(0)

    internal var galleryVersion by mutableIntStateOf(0)

    private val captureCounter = AtomicInteger()

    internal val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
            DeckLogger.log("Capture failed", tag = "Capture", payload = error.toString())
        },
    )

    public fun captureDirectory(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "devdeck").apply { mkdirs() }
    }

    public fun captures(context: Context): List<File> = captureDirectory(context)
        .listFiles { file -> file.isFile && (file.isCaptureImage || file.isCaptureVideo) }
        .orEmpty()
        .sortedByDescending { it.lastModified() }

    public fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.devdeck.files", file)

    internal fun newCaptureFile(context: Context, prefix: String, extension: String): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val directory = captureDirectory(context)
        var file = File(directory, "${prefix}_$stamp.$extension")
        while (file.exists()) file = File(directory, "${prefix}_${stamp}_${captureCounter.incrementAndGet()}.$extension")
        return file
    }

    internal fun notifyGalleryChanged() {
        galleryVersion++
    }

    public suspend fun takeScreenshot(activity: Activity): File? {
        val bitmap = withOverlaysHidden { captureWindow(activity) } ?: return null
        return savePng(activity, bitmap, "screenshot")
    }

    /**
     * Captures the screen's main scrolling content in one tall image (Android 12+), from what's on screen down,
     * within a few screens and seconds. Returns null when there is nothing to scroll or the capture failed; use
     * [takeScreenshot] then. Safe to call from any thread.
     */
    public suspend fun takeLongScreenshot(activity: Activity): File? = (longScreenshot(activity) as? LongScreenshotOutcome.Saved)?.file

    internal suspend fun longScreenshot(activity: Activity): LongScreenshotOutcome = withContext(Dispatchers.Main.immediate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@withContext LongScreenshotOutcome.NothingToScroll
        val result = withOverlaysHidden {
            val window = captureWindow(activity) ?: return@withOverlaysHidden LongScreenshot.Result.Failed
            try {
                LongScreenshot.capture(activity, window)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DeckLogger.log("Long screenshot failed", tag = "Capture", payload = error.toString())
                LongScreenshot.Result.Failed
            }
        }
        when (result) {
            is LongScreenshot.Result.Captured ->
                savePng(activity, result.bitmap, "long_screenshot")?.let { LongScreenshotOutcome.Saved(it) } ?: LongScreenshotOutcome.Failed

            LongScreenshot.Result.NothingToScroll -> LongScreenshotOutcome.NothingToScroll

            LongScreenshot.Result.Failed -> LongScreenshotOutcome.Failed
        }
    }

    private suspend fun <T> withOverlaysHidden(block: suspend () -> T): T {
        capturesInProgress++
        return try {
            // Give the overlay a frame to disappear before the window is copied.
            delay(OVERLAY_HIDE_MILLIS)
            block()
        } finally {
            capturesInProgress--
        }
    }

    private suspend fun savePng(activity: Activity, bitmap: Bitmap, prefix: String): File? {
        val file = newCaptureFile(activity, prefix, "png")
        val written = withContext(Dispatchers.IO) {
            try {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (_: IOException) {
                file.delete()
                false
            }
        }
        if (!written) return null
        notifyGalleryChanged()
        return file
    }

    private suspend fun captureWindow(activity: Activity): Bitmap? {
        val decorView = activity.window?.decorView ?: return null
        if (decorView.width == 0 || decorView.height == 0) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return runCatching { decorView.drawToBitmap() }.getOrNull()
        val bitmap = createBitmap(decorView.width, decorView.height)
        val result = try {
            suspendCancellableCoroutine<Int> { continuation ->
                PixelCopy.request(activity.window, bitmap, { continuation.resume(it) }, Handler(Looper.getMainLooper()))
            }
        } catch (_: IllegalArgumentException) {
            return null
        }
        return if (result == PixelCopy.SUCCESS) bitmap else null
    }

    internal fun startRecording(context: Context, resultCode: Int, data: Intent) {
        val intent = Intent(context, ScreenRecordingService::class.java)
            .setAction(ScreenRecordingService.ACTION_START)
            .putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(ScreenRecordingService.EXTRA_DATA, data)
        ContextCompat.startForegroundService(context, intent)
    }

    internal fun pauseRecording(context: Context) = sendRecordingAction(context, ScreenRecordingService.ACTION_PAUSE)

    internal fun resumeRecording(context: Context) = sendRecordingAction(context, ScreenRecordingService.ACTION_RESUME)

    private fun sendRecordingAction(context: Context, action: String) {
        context.startService(Intent(context, ScreenRecordingService::class.java).setAction(action))
    }

    internal fun stopRecording(context: Context) {
        context.startService(
            Intent(context, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_STOP),
        )
    }
}

private const val OVERLAY_HIDE_MILLIS = 100L
