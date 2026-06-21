package com.tejaslabs.beauty.ai.camera.ui

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File

sealed class Screen {
    object Splash : Screen()
    object Camera : Screen()
    object GalleryImport : Screen()
    data class Editor(
        val imageFile: File?,
        val imageUri: Uri?,
        val initialSmoothing: Float = 0.40f,
        val initialBrightness: Float = 0.0f,
        val initialSkinTone: Float = 0.20f,
        val initialEyeEnlargement: Float = 0.0f,
        val initialFaceSlimming: Float = 0.0f,
        val initialDarkCircleRemover: Float = 0.0f,
        val initialJawSharpening: Float = 0.0f,
        val initialNoseSlimming: Float = 0.0f,
        val initialLipColor: Float = 0.0f,
        val initialFilterType: Int = 0,
        val initialFilterIntensity: Float = 0.60f,
        val initialCropRatio: String = "Free"
    ) : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("BeautyCameraPrefs", android.content.Context.MODE_PRIVATE)
    }

    // Maintain state across screens, initialized from SharedPreferences (beauty options default to 0.0f)
    var activeSmoothing by remember { mutableStateOf(sharedPrefs.getFloat("smoothing", 0.0f)) }
    var activeBrightness by remember { mutableStateOf(sharedPrefs.getFloat("brightness", 0.0f)) }
    var activeSkinTone by remember { mutableStateOf(sharedPrefs.getFloat("skinTone", 0.0f)) }
    var activeEyeEnlargement by remember { mutableStateOf(sharedPrefs.getFloat("eyeEnlargement", 0.0f)) }
    var activeFaceSlimming by remember { mutableStateOf(sharedPrefs.getFloat("faceSlimming", 0.0f)) }
    var activeDarkCircleRemover by remember { mutableStateOf(sharedPrefs.getFloat("darkCircleRemover", 0.0f)) }
    var activeJawSharpening by remember { mutableStateOf(sharedPrefs.getFloat("jawSharpening", 0.0f)) }
    var activeNoseSlimming by remember { mutableStateOf(sharedPrefs.getFloat("noseSlimming", 0.0f)) }
    var activeLipColor by remember { mutableStateOf(sharedPrefs.getFloat("lipColor", 0.0f)) }
    var activeFilterType by remember { mutableIntStateOf(sharedPrefs.getInt("filterType", 0)) }
    var activeFilterIntensity by remember { mutableStateOf(sharedPrefs.getFloat("filterIntensity", 0.60f)) }

    var activeFlashMode by remember { mutableIntStateOf(sharedPrefs.getInt("flashMode", ImageCapture.FLASH_MODE_OFF)) }
    var activeTimerSeconds by remember { mutableIntStateOf(sharedPrefs.getInt("timerSeconds", 0)) }
    var activeIsGridEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("isGridEnabled", false)) }
    var activeAspectRatio by remember { mutableStateOf(sharedPrefs.getString("aspectRatio", "3:4") ?: "3:4") }

    // Automatically save values to SharedPreferences on change
    LaunchedEffect(
        activeSmoothing, activeBrightness, activeSkinTone,
        activeEyeEnlargement, activeFaceSlimming, activeDarkCircleRemover,
        activeJawSharpening, activeNoseSlimming, activeLipColor,
        activeFilterType, activeFilterIntensity,
        activeFlashMode, activeTimerSeconds, activeIsGridEnabled, activeAspectRatio
    ) {
        sharedPrefs.edit().apply {
            putFloat("smoothing", activeSmoothing)
            putFloat("brightness", activeBrightness)
            putFloat("skinTone", activeSkinTone)
            putFloat("eyeEnlargement", activeEyeEnlargement)
            putFloat("faceSlimming", activeFaceSlimming)
            putFloat("darkCircleRemover", activeDarkCircleRemover)
            putFloat("jawSharpening", activeJawSharpening)
            putFloat("noseSlimming", activeNoseSlimming)
            putFloat("lipColor", activeLipColor)
            putInt("filterType", activeFilterType)
            putFloat("filterIntensity", activeFilterIntensity)
            putInt("flashMode", activeFlashMode)
            putInt("timerSeconds", activeTimerSeconds)
            putBoolean("isGridEnabled", activeIsGridEnabled)
            putString("aspectRatio", activeAspectRatio)
            apply()
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = Screen.Camera
                    }
                )
            }
            is Screen.Camera -> {
                CameraScreen(
                    initialSmoothing = activeSmoothing,
                    initialBrightness = activeBrightness,
                    initialSkinTone = activeSkinTone,
                    initialEyeEnlargement = activeEyeEnlargement,
                    initialFaceSlimming = activeFaceSlimming,
                    initialDarkCircleRemover = activeDarkCircleRemover,
                    initialJawSharpening = activeJawSharpening,
                    initialNoseSlimming = activeNoseSlimming,
                    initialLipColor = activeLipColor,
                    initialFilterType = activeFilterType,
                    initialFilterIntensity = activeFilterIntensity,
                    initialFlashMode = activeFlashMode,
                    initialTimerSeconds = activeTimerSeconds,
                    initialIsGridEnabled = activeIsGridEnabled,
                    initialAspectRatio = activeAspectRatio,
                    onBeautySettingsChanged = { smoothing, brightness, tone, eye, face, darkCircle, jaw, nose, lip, filterType, filterIntensity ->
                        activeSmoothing = smoothing
                        activeBrightness = brightness
                        activeSkinTone = tone
                        activeEyeEnlargement = eye
                        activeFaceSlimming = face
                        activeDarkCircleRemover = darkCircle
                        activeJawSharpening = jaw
                        activeNoseSlimming = nose
                        activeLipColor = lip
                        activeFilterType = filterType
                        activeFilterIntensity = filterIntensity
                    },
                    onCameraSettingsChanged = { flash, timer, grid, aspect ->
                        activeFlashMode = flash
                        activeTimerSeconds = timer
                        activeIsGridEnabled = grid
                        activeAspectRatio = aspect
                    },
                    onOpenGallery = {
                        currentScreen = Screen.GalleryImport
                    },
                    onPhotoCaptured = { file, aspectRatio, smoothing, brightness, tone, eye, face, darkCircle, jaw, nose, lip, filterType, filterIntensity ->
                        // Cache values to keep in sync
                        activeSmoothing = smoothing
                        activeBrightness = brightness
                        activeSkinTone = tone
                        activeEyeEnlargement = eye
                        activeFaceSlimming = face
                        activeDarkCircleRemover = darkCircle
                        activeJawSharpening = jaw
                        activeNoseSlimming = nose
                        activeLipColor = lip
                        activeFilterType = filterType
                        activeFilterIntensity = filterIntensity
                        activeAspectRatio = aspectRatio
 
                        currentScreen = Screen.Editor(
                            imageFile = file,
                            imageUri = null,
                            initialSmoothing = smoothing,
                            initialBrightness = brightness * 100f, // map -0.5..0.5 to -50..50
                            initialSkinTone = tone,
                            initialEyeEnlargement = eye,
                            initialFaceSlimming = face,
                            initialDarkCircleRemover = darkCircle,
                            initialJawSharpening = jaw,
                            initialNoseSlimming = nose,
                            initialLipColor = lip,
                            initialFilterType = filterType,
                            initialFilterIntensity = filterIntensity,
                            initialCropRatio = aspectRatio
                        )
                    }
                )
            }
            is Screen.GalleryImport -> {
                GalleryImportScreen(
                    onBack = {
                        currentScreen = Screen.Camera
                    },
                    onPhotoSelected = { uri ->
                        currentScreen = Screen.Editor(
                            imageFile = null,
                            imageUri = uri
                        )
                    }
                )
            }
            is Screen.Editor -> {
                EditorScreen(
                    imageFile = screen.imageFile,
                    imageUri = screen.imageUri,
                    initialSmoothing = screen.initialSmoothing,
                    initialBrightness = screen.initialBrightness,
                    initialSkinTone = screen.initialSkinTone,
                    initialEyeEnlargement = screen.initialEyeEnlargement,
                    initialFaceSlimming = screen.initialFaceSlimming,
                    initialDarkCircleRemover = screen.initialDarkCircleRemover,
                    initialJawSharpening = screen.initialJawSharpening,
                    initialNoseSlimming = screen.initialNoseSlimming,
                    initialLipColor = screen.initialLipColor,
                    initialFilterType = screen.initialFilterType,
                    initialFilterIntensity = screen.initialFilterIntensity,
                    initialCropRatio = screen.initialCropRatio,
                    onBack = {
                        currentScreen = Screen.Camera
                    }
                )
            }
        }
    }
}
