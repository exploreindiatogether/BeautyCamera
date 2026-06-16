package com.tejaslabs.beauty.ai.camera.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tejaslabs.beauty.ai.camera.detector.FaceDetector
import com.tejaslabs.beauty.ai.camera.detector.FaceLandmarksData
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
 
class CameraGLView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
 
    private val TAG = "CameraGLView"
 
    private val renderer: CameraGLRenderer
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null

    init {
        // Configure OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = CameraGLRenderer { surfaceTexture ->
            // On GL thread, when the SurfaceTexture is ready, we bind CameraX preview to it
            post {
                setupCamera(surfaceTexture)
            }
        }
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        // Set up the listener on the renderer to trigger render passes when frames arrive
        renderer.apply {
            // We manually request render on frame available
        }
    }

    override fun onResume() {
        super.onResume()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onPause() {
        super.onPause()
        cameraExecutor.shutdown()
        faceDetector?.close()
        faceDetector = null
    }

    // Setters for beauty parameters (delegates to GL renderer)
    fun setSmoothing(value: Float) {
        renderer.smoothing = value
    }

    fun setBrightness(value: Float) {
        renderer.brightness = value
    }

    fun setSkinTone(value: Float) {
        renderer.skinTone = value
    }

    fun setEyeEnlargement(value: Float) {
        renderer.eyeEnlargement = value
    }

    fun setFaceSlimming(value: Float) {
        renderer.faceSlimming = value
    }

    fun setFilter(type: Int, intensity: Float) {
        renderer.filterType = type
        renderer.filterIntensity = intensity
    }

    fun setCameraFacing(isFront: Boolean) {
        if (renderer.isFrontCamera != isFront) {
            renderer.isFrontCamera = isFront
            renderer.landmarks = null // Clear landmarks to prevent ghost warping
            rebindCamera()
        }
    }

    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
        val enableTorch = (flashMode == ImageCapture.FLASH_MODE_ON)
        camera?.cameraControl?.enableTorch(enableTorch)
    }

    private var lifecycleOwner: LifecycleOwner? = null

    fun bindToLifecycle(owner: LifecycleOwner) {
        lifecycleOwner = owner
        rebindCamera()
    }

    private fun rebindCamera() {
        val owner = lifecycleOwner ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            // If EGL surface is already created, setup camera
            val st = renderer.surfaceTexture
            if (st != null) {
                setupCamera(st)
            }
            // Trigger setup camera if renderer already generated surfaceTexture
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupCamera(surfaceTexture: SurfaceTexture) {
        val owner = lifecycleOwner ?: return
        val provider = cameraProvider ?: return

        surfaceTexture.setOnFrameAvailableListener {
            requestRender()
        }

        try {
            provider.unbindAll()

            // Preview Use Case
            preview = Preview.Builder()
                .setTargetRotation(display.rotation)
                .build()

            preview?.setSurfaceProvider { request ->
                val resolution = request.resolution
                renderer.cameraWidth = resolution.width.toFloat()
                renderer.cameraHeight = resolution.height.toFloat()
                surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
                val surface = android.view.Surface(surfaceTexture)
                request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {
                    surface.release()
                }
            }

            // Image Capture Use Case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(display.rotation)
                .build()

            // Initialize FaceDetector if not already created
            if (faceDetector == null) {
                faceDetector = FaceDetector(context) { data ->
                    renderer.landmarks = data
                    requestRender()
                }
            }

            // Image Analysis Use Case (for Face Mesh tracking)
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(display.rotation)
                .build()

            imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                faceDetector?.detect(imageProxy)
            }

            val cameraSelector = if (renderer.isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            camera = provider.bindToLifecycle(
                owner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            // Enable torch if flash mode is ON
            val isFlashOn = (imageCapture?.flashMode == ImageCapture.FLASH_MODE_ON)
            camera?.cameraControl?.enableTorch(isFlashOn)
            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
        }
    }

    fun capturePhoto(onPhotoCaptured: (File) -> Unit, onError: (Exception) -> Unit) {
        val capture = imageCapture ?: run {
            onError(Exception("Camera not ready"))
            return
        }

        val outputFile = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}
