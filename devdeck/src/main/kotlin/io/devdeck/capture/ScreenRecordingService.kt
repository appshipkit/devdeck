package io.devdeck.capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import io.devdeck.logs.DeckLogger
import java.io.File

public class ScreenRecordingService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = IntentCompat.getParcelableExtra(intent, EXTRA_DATA, Intent::class.java)
                if (data == null || projection != null) {
                    if (projection == null) {
                        startInForeground()
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf()
                } else {
                    start(resultCode, data)
                }
            }

            ACTION_STOP -> finish()
        }
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0,
        )
    }

    private fun start(resultCode: Int, data: Intent) {
        startInForeground()
        val started = runCatching {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, data) ?: error("Projection unavailable")
            projection = mediaProjection
            mediaProjection.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() = finish()
                },
                Handler(Looper.getMainLooper()),
            )

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics)
            val scale = (MAX_VIDEO_WIDTH.toFloat() / metrics.widthPixels).coerceAtMost(1f)
            val width = (metrics.widthPixels * scale).toInt() and 1.inv()
            val height = (metrics.heightPixels * scale).toInt() and 1.inv()

            val file = ScreenCapture.newCaptureFile(this, "recording", "mp4")
            outputFile = file
            val mediaRecorder = if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = mediaRecorder
            mediaRecorder.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)
                setOutputFile(file.absolutePath)
                prepare()
            }
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "DevDeckRecording",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null,
            )
            mediaRecorder.start()
        }
        if (started.isSuccess) {
            ScreenCapture.isRecording = true
            DeckLogger.log("Screen recording started", tag = LOG_TAG)
        } else {
            DeckLogger.log("Screen recording failed", tag = LOG_TAG, payload = started.exceptionOrNull()?.toString())
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
            outputFile?.delete()
            outputFile = null
            finish()
        }
    }

    private fun finish() {
        val file = outputFile
        val saved = recorder?.let { runCatching { it.stop() }.isSuccess } == true
        recorder?.release()
        recorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null
        outputFile = null
        if (file != null) {
            if (saved) {
                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
                DeckLogger.log("Screen recording saved", tag = LOG_TAG, payload = file.name)
            } else {
                file.delete()
            }
            ScreenCapture.notifyGalleryChanged()
        }
        ScreenCapture.isRecording = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (projection != null) finish()
        super.onDestroy()
    }

    private fun buildNotification() = run {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Screen recording", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Recording screen")
            .setContentText("Tap Stop to save the recording")
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    internal companion object {
        const val ACTION_START = "io.devdeck.action.START_RECORDING"
        const val ACTION_STOP = "io.devdeck.action.STOP_RECORDING"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        private const val CHANNEL_ID = "devdeck_recording"
        private const val NOTIFICATION_ID = 0x0DEB
        private const val MAX_VIDEO_WIDTH = 720
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_BIT_RATE = 6_000_000
        private const val LOG_TAG = "Screen capture"
    }
}
