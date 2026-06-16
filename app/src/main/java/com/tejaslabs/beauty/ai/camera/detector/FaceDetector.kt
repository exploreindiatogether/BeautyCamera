package com.tejaslabs.beauty.ai.camera.detector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions

class FaceDetector(
    private val context: Context,
    private val onLandmarksDetected: (FaceLandmarksData?) -> Unit
) {
    private var faceLandmarker: FaceLandmarker? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        executor.execute {
            try {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()

                val options = FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ ->
                        processResult(result)
                    }
                    .setErrorListener { error ->
                        Log.e("FaceDetector", "MediaPipe Error: ${error.message}", error)
                    }
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinFacePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()

                faceLandmarker = FaceLandmarker.createFromOptions(context, options)
                Log.d("FaceDetector", "FaceLandmarker created successfully")
            } catch (e: Exception) {
                Log.e("FaceDetector", "Failed to create FaceLandmarker", e)
            }
        }
    }

    fun detect(imageProxy: ImageProxy) {
        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        executor.execute {
            try {
                val bitmap = imageProxy.toBitmap()
                val mpImage = BitmapImageBuilder(bitmap).build()
                val timestampMs = SystemClock.uptimeMillis()
                
                // imageProxy.toBitmap() already rotates the bitmap to upright,
                // so we do not need to rotate it again for MediaPipe.
                val imageProcessingOptions = ImageProcessingOptions.builder()
                    .setRotationDegrees(0)
                    .build()
                
                landmarker.detectAsync(mpImage, imageProcessingOptions, timestampMs)
            } catch (e: Exception) {
                Log.e("FaceDetector", "Error in detection", e)
            } finally {
                imageProxy.close()
            }
        }
    }

    companion object {
        private const val TAG = "FaceDetector"

        fun detectStatic(context: Context, bitmap: Bitmap): FaceLandmarksData? {
            var scaledBitmap: Bitmap? = null
            var argbBitmap: Bitmap? = null
            var staticLandmarker: FaceLandmarker? = null
            try {
                // 1. Scale down to a reasonable size to prevent native OOM / SIGBUS
                val maxDim = 512
                val srcWidth = bitmap.width
                val srcHeight = bitmap.height
                val scale = maxDim.toFloat() / Math.max(srcWidth, srcHeight).coerceAtLeast(1)
                scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (srcWidth * scale).toInt().coerceAtLeast(1),
                        (srcHeight * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }

                // 2. Ensure config is ARGB_8888 (required/preferred by MediaPipe) and NOT hardware
                argbBitmap = if (scaledBitmap.config != Bitmap.Config.ARGB_8888) {
                    scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    scaledBitmap
                }

                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()

                val options = FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.5f)
                    .build()

                staticLandmarker = FaceLandmarker.createFromOptions(context, options)
                val mpImage = BitmapImageBuilder(argbBitmap).build()
                val result = staticLandmarker.detect(mpImage)
                
                return parseStaticLandmarks(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to run static face mesh", e)
                return null
            } finally {
                try {
                    staticLandmarker?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing static landmarker", e)
                }
                if (scaledBitmap != null && scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                if (argbBitmap != null && argbBitmap != bitmap && argbBitmap != scaledBitmap) {
                    argbBitmap.recycle()
                }
            }
        }

        private fun parseStaticLandmarks(result: FaceLandmarkerResult): FaceLandmarksData? {
            val landmarksList = result.faceLandmarks()
            if (landmarksList.isNullOrEmpty() || landmarksList[0].isNullOrEmpty()) {
                return null
            }

            val landmarks = landmarksList[0]

            // Left eye outer: 33, inner: 133
            // Right eye outer: 263, inner: 362
            // Nose tip: 4
            val leftEyeOuter = landmarks[33]
            val leftEyeInner = landmarks[133]
            val rightEyeOuter = landmarks[263]
            val rightEyeInner = landmarks[362]
            val noseTip = landmarks[4]

            val leftEyeX = (leftEyeOuter.x() + leftEyeInner.x()) / 2f
            val leftEyeY = (leftEyeOuter.y() + leftEyeInner.y()) / 2f
            val rightEyeX = (rightEyeOuter.x() + rightEyeInner.x()) / 2f
            val rightEyeY = (rightEyeOuter.y() + rightEyeInner.y()) / 2f
            val noseX = noseTip.x()
            val noseY = noseTip.y()

            return FaceLandmarksData(
                leftEyeX = leftEyeX,
                leftEyeY = leftEyeY,
                rightEyeX = rightEyeX,
                rightEyeY = rightEyeY,
                noseX = noseX,
                noseY = noseY
            )
        }
    }


    private fun processResult(result: FaceLandmarkerResult) {
        val data = parseLandmarks(result, bypassSmooth = false)
        onLandmarksDetected(data)
    }

    private var lastData: FaceLandmarksData? = null
    private val alpha = 0.25f // Smoothing factor for low-pass EMA filter

    private fun parseLandmarks(result: FaceLandmarkerResult, bypassSmooth: Boolean): FaceLandmarksData? {
        val landmarksList = result.faceLandmarks()
        if (landmarksList.isNullOrEmpty() || landmarksList[0].isNullOrEmpty()) {
            return null
        }

        val landmarks = landmarksList[0]

        // Left eye outer: 33, inner: 133
        // Right eye outer: 263, inner: 362
        // Nose tip: 4
        val leftEyeOuter = landmarks[33]
        val leftEyeInner = landmarks[133]
        val rightEyeOuter = landmarks[263]
        val rightEyeInner = landmarks[362]
        val noseTip = landmarks[4]

        var leftEyeX = (leftEyeOuter.x() + leftEyeInner.x()) / 2f
        var leftEyeY = (leftEyeOuter.y() + leftEyeInner.y()) / 2f
        var rightEyeX = (rightEyeOuter.x() + rightEyeInner.x()) / 2f
        var rightEyeY = (rightEyeOuter.y() + rightEyeInner.y()) / 2f
        var noseX = noseTip.x()
        var noseY = noseTip.y()

        val prev = lastData
        if (prev != null && !bypassSmooth) {
            leftEyeX = prev.leftEyeX + alpha * (leftEyeX - prev.leftEyeX)
            leftEyeY = prev.leftEyeY + alpha * (leftEyeY - prev.leftEyeY)
            rightEyeX = prev.rightEyeX + alpha * (rightEyeX - prev.rightEyeX)
            rightEyeY = prev.rightEyeY + alpha * (rightEyeY - prev.rightEyeY)
            noseX = prev.noseX + alpha * (noseX - prev.noseX)
            noseY = prev.noseY + alpha * (noseY - prev.noseY)
        }

        val currentData = FaceLandmarksData(
            leftEyeX = leftEyeX,
            leftEyeY = leftEyeY,
            rightEyeX = rightEyeX,
            rightEyeY = rightEyeY,
            noseX = noseX,
            noseY = noseY
        )
        if (!bypassSmooth) {
            lastData = currentData
        }
        return currentData
    }

    fun close() {
        executor.execute {
            faceLandmarker?.close()
            faceLandmarker = null
        }
        executor.shutdown()
    }
}

data class FaceLandmarksData(
    val leftEyeX: Float,
    val leftEyeY: Float,
    val rightEyeX: Float,
    val rightEyeY: Float,
    val noseX: Float,
    val noseY: Float
)
