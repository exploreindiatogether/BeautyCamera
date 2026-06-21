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

            // Eye landmarks
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

            // Under-eye landmarks (lower eyelid center area)
            val underEyeLeft = landmarks[110]
            val underEyeRight = landmarks[339]

            // Lip landmarks
            val lipTop = landmarks[13]
            val lipBottom = landmarks[14]
            val lipLeft = landmarks[61]
            val lipRight = landmarks[291]

            // Jaw landmarks
            val jawLeft = landmarks[234]
            val jawRight = landmarks[454]

            // Nose bridge
            val noseBridgeTop = landmarks[6]

            // Forehead and Chin
            val forehead = landmarks[10]
            val chin = landmarks[152]

            return FaceLandmarksData(
                leftEyeX = leftEyeX,
                leftEyeY = leftEyeY,
                rightEyeX = rightEyeX,
                rightEyeY = rightEyeY,
                noseX = noseX,
                noseY = noseY,
                underEyeLeftX = underEyeLeft.x(),
                underEyeLeftY = underEyeLeft.y(),
                underEyeRightX = underEyeRight.x(),
                underEyeRightY = underEyeRight.y(),
                lipTopX = lipTop.x(),
                lipTopY = lipTop.y(),
                lipBottomX = lipBottom.x(),
                lipBottomY = lipBottom.y(),
                lipLeftX = lipLeft.x(),
                lipLeftY = lipLeft.y(),
                lipRightX = lipRight.x(),
                lipRightY = lipRight.y(),
                jawLeftX = jawLeft.x(),
                jawLeftY = jawLeft.y(),
                jawRightX = jawRight.x(),
                jawRightY = jawRight.y(),
                noseBridgeTopX = noseBridgeTop.x(),
                noseBridgeTopY = noseBridgeTop.y(),
                foreheadX = forehead.x(),
                foreheadY = forehead.y(),
                chinX = chin.x(),
                chinY = chin.y()
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

        // Eye landmarks
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

        // Under-eye landmarks
        val underEyeLeft = landmarks[110]
        val underEyeRight = landmarks[339]
        var underEyeLeftX = underEyeLeft.x()
        var underEyeLeftY = underEyeLeft.y()
        var underEyeRightX = underEyeRight.x()
        var underEyeRightY = underEyeRight.y()

        // Lip landmarks
        val lipTop = landmarks[13]
        val lipBottom = landmarks[14]
        val lipLeft = landmarks[61]
        val lipRight = landmarks[291]
        var lipTopX = lipTop.x()
        var lipTopY = lipTop.y()
        var lipBottomX = lipBottom.x()
        var lipBottomY = lipBottom.y()
        var lipLeftX = lipLeft.x()
        var lipLeftY = lipLeft.y()
        var lipRightX = lipRight.x()
        var lipRightY = lipRight.y()

        // Jaw landmarks
        val jawLeft = landmarks[234]
        val jawRight = landmarks[454]
        var jawLeftX = jawLeft.x()
        var jawLeftY = jawLeft.y()
        var jawRightX = jawRight.x()
        var jawRightY = jawRight.y()

        // Nose bridge
        val noseBridgeTop = landmarks[6]
        var noseBridgeTopX = noseBridgeTop.x()
        var noseBridgeTopY = noseBridgeTop.y()

        // Forehead and Chin
        val forehead = landmarks[10]
        val chin = landmarks[152]
        var foreheadX = forehead.x()
        var foreheadY = forehead.y()
        var chinX = chin.x()
        var chinY = chin.y()

        val prev = lastData
        if (prev != null && !bypassSmooth) {
            leftEyeX = prev.leftEyeX + alpha * (leftEyeX - prev.leftEyeX)
            leftEyeY = prev.leftEyeY + alpha * (leftEyeY - prev.leftEyeY)
            rightEyeX = prev.rightEyeX + alpha * (rightEyeX - prev.rightEyeX)
            rightEyeY = prev.rightEyeY + alpha * (rightEyeY - prev.rightEyeY)
            noseX = prev.noseX + alpha * (noseX - prev.noseX)
            noseY = prev.noseY + alpha * (noseY - prev.noseY)
            underEyeLeftX = prev.underEyeLeftX + alpha * (underEyeLeftX - prev.underEyeLeftX)
            underEyeLeftY = prev.underEyeLeftY + alpha * (underEyeLeftY - prev.underEyeLeftY)
            underEyeRightX = prev.underEyeRightX + alpha * (underEyeRightX - prev.underEyeRightX)
            underEyeRightY = prev.underEyeRightY + alpha * (underEyeRightY - prev.underEyeRightY)
            lipTopX = prev.lipTopX + alpha * (lipTopX - prev.lipTopX)
            lipTopY = prev.lipTopY + alpha * (lipTopY - prev.lipTopY)
            lipBottomX = prev.lipBottomX + alpha * (lipBottomX - prev.lipBottomX)
            lipBottomY = prev.lipBottomY + alpha * (lipBottomY - prev.lipBottomY)
            lipLeftX = prev.lipLeftX + alpha * (lipLeftX - prev.lipLeftX)
            lipLeftY = prev.lipLeftY + alpha * (lipLeftY - prev.lipLeftY)
            lipRightX = prev.lipRightX + alpha * (lipRightX - prev.lipRightX)
            lipRightY = prev.lipRightY + alpha * (lipRightY - prev.lipRightY)
            jawLeftX = prev.jawLeftX + alpha * (jawLeftX - prev.jawLeftX)
            jawLeftY = prev.jawLeftY + alpha * (jawLeftY - prev.jawLeftY)
            jawRightX = prev.jawRightX + alpha * (jawRightX - prev.jawRightX)
            jawRightY = prev.jawRightY + alpha * (jawRightY - prev.jawRightY)
            noseBridgeTopX = prev.noseBridgeTopX + alpha * (noseBridgeTopX - prev.noseBridgeTopX)
            noseBridgeTopY = prev.noseBridgeTopY + alpha * (noseBridgeTopY - prev.noseBridgeTopY)
            foreheadX = prev.foreheadX + alpha * (foreheadX - prev.foreheadX)
            foreheadY = prev.foreheadY + alpha * (foreheadY - prev.foreheadY)
            chinX = prev.chinX + alpha * (chinX - prev.chinX)
            chinY = prev.chinY + alpha * (chinY - prev.chinY)
        }

        val currentData = FaceLandmarksData(
            leftEyeX = leftEyeX,
            leftEyeY = leftEyeY,
            rightEyeX = rightEyeX,
            rightEyeY = rightEyeY,
            noseX = noseX,
            noseY = noseY,
            underEyeLeftX = underEyeLeftX,
            underEyeLeftY = underEyeLeftY,
            underEyeRightX = underEyeRightX,
            underEyeRightY = underEyeRightY,
            lipTopX = lipTopX,
            lipTopY = lipTopY,
            lipBottomX = lipBottomX,
            lipBottomY = lipBottomY,
            lipLeftX = lipLeftX,
            lipLeftY = lipLeftY,
            lipRightX = lipRightX,
            lipRightY = lipRightY,
            jawLeftX = jawLeftX,
            jawLeftY = jawLeftY,
            jawRightX = jawRightX,
            jawRightY = jawRightY,
            noseBridgeTopX = noseBridgeTopX,
            noseBridgeTopY = noseBridgeTopY,
            foreheadX = foreheadX,
            foreheadY = foreheadY,
            chinX = chinX,
            chinY = chinY
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
    val noseY: Float,
    // Under-eye center points (for dark circle removal)
    val underEyeLeftX: Float = 0f,
    val underEyeLeftY: Float = 0f,
    val underEyeRightX: Float = 0f,
    val underEyeRightY: Float = 0f,
    // Lip contour points (for lip color)
    val lipTopX: Float = 0f,
    val lipTopY: Float = 0f,
    val lipBottomX: Float = 0f,
    val lipBottomY: Float = 0f,
    val lipLeftX: Float = 0f,
    val lipLeftY: Float = 0f,
    val lipRightX: Float = 0f,
    val lipRightY: Float = 0f,
    // Jaw points (for jaw sharpening)
    val jawLeftX: Float = 0f,
    val jawLeftY: Float = 0f,
    val jawRightX: Float = 0f,
    val jawRightY: Float = 0f,
    // Nose bridge (for nose slimming)
    val noseBridgeTopX: Float = 0f,
    val noseBridgeTopY: Float = 0f,
    // Forehead and chin
    val foreheadX: Float = 0f,
    val foreheadY: Float = 0f,
    val chinX: Float = 0f,
    val chinY: Float = 0f
)
