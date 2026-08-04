package com.sentinelshield.antitheft

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntruderActivity : ComponentActivity() {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isStopping = false
    private var cameraRetryCount = 0
    private var stopBackupRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_intruder)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Log.w("IntruderActivity", "Camera permission missing, finishing activity.")
            com.sentinelshield.antitheft.utils.DebugLogger.log(this, "IntruderActivity", "Camera permission missing! Cannot capture selfie/video.", force = true)
            android.widget.Toast.makeText(this, "⚠️ Camera permission missing! Grant Camera permission first.", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val captureMode = SecurityPreferences.getIntruderCaptureMode(this)
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()

                if (captureMode == "VIDEO") {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    videoCapture = VideoCapture.withOutput(recorder)
                    
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, videoCapture
                    )
                    startVideoRecording()
                } else {
                    imageCapture = ImageCapture.Builder().build()
                    
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, imageCapture
                    )
                    // Short delay to allow camera sensor to adjust exposure before taking photo
                    handler.postDelayed({ takePhoto() }, 1000)
                }

            } catch (exc: Exception) {
                Log.e("IntruderActivity", "Use case binding failed (attempt $cameraRetryCount)", exc)
                if (cameraRetryCount < 3) {
                    cameraRetryCount++
                    handler.postDelayed({ startCamera() }, 500L * cameraRetryCount)
                } else {
                    finish()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (isFinishing || isDestroyed) return
        val imageCapture = this.imageCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SentinelShield")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("IntruderActivity", "Photo capture failed: ${exc.message}", exc)
                    finish()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("IntruderActivity", "Photo capture succeeded: ${output.savedUri}")
                    com.sentinelshield.antitheft.utils.DebugLogger.log(this@IntruderActivity, "IntruderActivity", "Photo captured: ${output.savedUri}", force = true)
                    output.savedUri?.let { uri ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val path = uri.path ?: ""
                            val file = java.io.File(path)
                            com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.uploadFile(this@IntruderActivity, file, "image/jpeg")
                        }
                    }
                    if (intent.getBooleanExtra("IS_TEST_MODE", false)) {
                        android.widget.Toast.makeText(this@IntruderActivity, "📸 Intruder Test Photo Saved to Gallery!", android.widget.Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            }
        )
    }

    private fun startVideoRecording() {
        if (isFinishing || isDestroyed) return
        val videoCapture = this.videoCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/SentinelShield")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val durationSeconds = SecurityPreferences.getIntruderVideoDuration(this)
        val durationNanos = durationSeconds * 1000_000_000L

        // Safety backup timer in case status ticks skip or freeze
        stopBackupRunnable = Runnable {
            if (!isStopping) {
                isStopping = true
                try {
                    recording?.stop()
                } catch (_: Exception) {}
                recording = null
            }
        }

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // Schedule backup stop after duration + 1.5s startup padding
                        stopBackupRunnable?.let { handler.postDelayed(it, (durationSeconds * 1000L) + 1500L) }
                    }
                    is VideoRecordEvent.Status -> {
                        if (!isStopping && recordEvent.recordingStats.recordedDurationNanos >= durationNanos) {
                            isStopping = true
                            stopBackupRunnable?.let { handler.removeCallbacks(it) }
                            try {
                                recording?.stop()
                            } catch (_: Exception) {}
                            recording = null
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        stopBackupRunnable?.let { handler.removeCallbacks(it) }
                        if (recordEvent.hasError()) {
                            Log.e("IntruderActivity", "Video capture ends with error: ${recordEvent.error}")
                            com.sentinelshield.antitheft.utils.DebugLogger.log(this@IntruderActivity, "IntruderActivity", "Video capture failed: ${recordEvent.error}", force = true)
                        } else {
                            Log.d("IntruderActivity", "Video capture succeeded: ${recordEvent.outputResults.outputUri}")
                            com.sentinelshield.antitheft.utils.DebugLogger.log(this@IntruderActivity, "IntruderActivity", "Video captured: ${recordEvent.outputResults.outputUri}", force = true)
                            recordEvent.outputResults.outputUri.let { uri ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val path = uri.path ?: ""
                                    val file = java.io.File(path)
                                    com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.uploadFile(this@IntruderActivity, file, "video/mp4")
                                }
                            }
                            if (intent.getBooleanExtra("IS_TEST_MODE", false)) {
                                android.widget.Toast.makeText(this@IntruderActivity, "🎥 Intruder Test Video Saved to Gallery!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        finish()
                    }
                }
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        stopBackupRunnable?.let { handler.removeCallbacks(it) }
        if (!isStopping) {
            isStopping = true
            try {
                recording?.stop()
            } catch (_: Exception) {}
        }
        recording = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
