package com.tejaslabs.beauty.ai.camera.ui

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.tejaslabs.beauty.ai.camera.gallery.GalleryHelper
import com.tejaslabs.beauty.ai.camera.renderer.CameraGLView
import java.io.File

// Data class representing a beauty adjustment item
data class FeatureItem(
    val id: String,
    val label: String,
    val icon: FeatureIcon,
    val min: Float = 0f,
    val max: Float = 1f,
    val type: String = "slider", // "slider", "presets_skin", "presets_face"
    val getValue: () -> Float,
    val onValueChange: (Float) -> Unit
)

@Composable
fun CameraScreen(
    initialSmoothing: Float,
    initialBrightness: Float,
    initialSkinTone: Float,
    initialEyeEnlargement: Float,
    initialFaceSlimming: Float,
    initialDarkCircleRemover: Float,
    initialJawSharpening: Float,
    initialNoseSlimming: Float,
    initialLipColor: Float,
    initialFilterType: Int,
    initialFilterIntensity: Float,
    initialFlashMode: Int,
    initialTimerSeconds: Int,
    initialIsGridEnabled: Boolean,
    initialAspectRatio: String,
    onBeautySettingsChanged: (
        smoothing: Float,
        brightness: Float,
        skinTone: Float,
        eyeEnlargement: Float,
        faceSlimming: Float,
        darkCircleRemover: Float,
        jawSharpening: Float,
        noseSlimming: Float,
        lipColor: Float,
        filterType: Int,
        filterIntensity: Float
    ) -> Unit,
    onCameraSettingsChanged: (
        flashMode: Int,
        timerSeconds: Int,
        isGridEnabled: Boolean,
        aspectRatio: String
    ) -> Unit,
    onOpenGallery: () -> Unit,
    onPhotoCaptured: (
        file: File,
        aspectRatio: String,
        smoothing: Float,
        brightness: Float,
        skinTone: Float,
        eyeEnlargement: Float,
        faceSlimming: Float,
        darkCircleRemover: Float,
        jawSharpening: Float,
        noseSlimming: Float,
        lipColor: Float,
        filterType: Int,
        filterIntensity: Float
    ) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reference to custom GL Camera View
    var cameraView: CameraGLView? by remember { mutableStateOf(null) }

    // Core Control States (bind directly to renderer)
    var smoothing by remember { mutableFloatStateOf(initialSmoothing) }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }
    var skinTone by remember { mutableFloatStateOf(initialSkinTone) }
    var eyeEnlargement by remember { mutableFloatStateOf(initialEyeEnlargement) }
    var faceSlimming by remember { mutableFloatStateOf(initialFaceSlimming) }
    var darkCircleRemover by remember { mutableFloatStateOf(initialDarkCircleRemover) }
    var jawSharpening by remember { mutableFloatStateOf(initialJawSharpening) }
    var noseSlimming by remember { mutableFloatStateOf(initialNoseSlimming) }
    var lipColor by remember { mutableFloatStateOf(initialLipColor) }
    
    var filterType by remember { mutableIntStateOf(initialFilterType) }
    var filterIntensity by remember { mutableFloatStateOf(initialFilterIntensity) }

    var isFrontCamera by remember { mutableStateOf(true) }
    var flashMode by remember { mutableIntStateOf(initialFlashMode) }
    var timerSeconds by remember { mutableIntStateOf(initialTimerSeconds) }
    var isGridEnabled by remember { mutableStateOf(initialIsGridEnabled) }
    
    // UI Panel States
    var activeTab by remember { mutableStateOf("Beauty") } // Beauty, Advanced, Makeup
    var activeAdvancedSubTab by remember { mutableStateOf("Face") } // Face, Eyes, Nose, Mouth
    var isPanelVisible by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showMoreOptionsDialog by remember { mutableStateOf(false) }
    
    androidx.activity.compose.BackHandler(enabled = isPanelVisible || showFilterPanel || showTimerDialog || showAspectRatioDialog || showMoreOptionsDialog) {
        when {
            showTimerDialog -> showTimerDialog = false
            showAspectRatioDialog -> showAspectRatioDialog = false
            showMoreOptionsDialog -> showMoreOptionsDialog = false
            showFilterPanel -> showFilterPanel = false
            isPanelVisible -> isPanelVisible = false
        }
    }
    var activeAspectRatio by remember { mutableStateOf(initialAspectRatio) }
    
    // Countdown State
    var countdownRemaining by remember { mutableIntStateOf(-1) }
    var isCountingDown by remember { mutableStateOf(false) }

    // Fetch latest gallery photo thumbnail
    var latestPhotoUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(key1 = true) {
        com.tejaslabs.beauty.ai.camera.firebase.FirebaseManager.logEvent("camera_screen_view")
        val photos = GalleryHelper.fetchPhotos(context)
        if (photos.isNotEmpty()) {
            latestPhotoUri = photos.first().uri
        }
    }

    // SharedPreferences for UI-only features persistence
    val sharedPrefs = remember {
        context.getSharedPreferences("BeautyCameraPrefs", android.content.Context.MODE_PRIVATE)
    }
    val uiOnlyFeatures = remember {
        mutableStateMapOf<String, Float>().apply {
            val keys = listOf(
                "headSize", "acne", "smileLines", "oiliness", "whiten", "softHair",
                "faceLength", "faceMiddle", "faceTop", "foreheadSmooth", "faceLift",
                "cheek", "sculpting", "mandible", "chin", "jaw",
                "eyeAngle", "eyeDistance", "nose3D", "noseTip",
                "mouthSize", "philtrum", "augmentation", "mouth3D",
                "makeupNone", "makeupDaily", "makeupPinky", "makeupPeach", "makeupRedLips"
            )
            keys.forEach { key ->
                this[key] = sharedPrefs.getFloat(key, 0.0f)
            }
        }
    }

    // Computed effective parameters blending core and advanced/sub-feature adjustments
    val effectiveSmoothing = (smoothing + 
            (uiOnlyFeatures["acne"] ?: 0f) * 0.4f + 
            (uiOnlyFeatures["foreheadSmooth"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["softHair"] ?: 0f) * 0.2f).coerceIn(0f, 1f)

    val effectiveFaceSlimming = (faceSlimming + 
            (uiOnlyFeatures["headSize"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["faceLength"] ?: 0f) * 0.2f + 
            (uiOnlyFeatures["faceMiddle"] ?: 0f) * 0.2f + 
            (uiOnlyFeatures["cheek"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["sculpting"] ?: 0f) * 0.3f).coerceIn(0f, 1f)

    val effectiveJawSharpening = (jawSharpening + 
            (uiOnlyFeatures["faceLift"] ?: 0f) * 0.4f + 
            (uiOnlyFeatures["mandible"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["chin"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["jaw"] ?: 0f) * 0.3f).coerceIn(0f, 1f)

    val effectiveEyeEnlargement = (eyeEnlargement + 
            (uiOnlyFeatures["eyeAngle"] ?: 0f) * 0.2f + 
            (uiOnlyFeatures["eyeDistance"] ?: 0f) * 0.2f).coerceIn(0f, 1f)

    val effectiveNoseSlimming = (noseSlimming + 
            (uiOnlyFeatures["nose3D"] ?: 0f) * 0.4f + 
            (uiOnlyFeatures["noseTip"] ?: 0f) * 0.3f).coerceIn(0f, 1f)

    val effectiveDarkCircleRemover = (darkCircleRemover + 
            (uiOnlyFeatures["smileLines"] ?: 0f) * 0.4f).coerceIn(0f, 1f)

    // Handle makeup offsets
    val makeupLipOffset = when {
        (uiOnlyFeatures["makeupDaily"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupDaily"] ?: 0f) * 0.3f
        (uiOnlyFeatures["makeupPinky"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupPinky"] ?: 0f) * 0.5f
        (uiOnlyFeatures["makeupPeach"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupPeach"] ?: 0f) * 0.4f
        (uiOnlyFeatures["makeupRedLips"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupRedLips"] ?: 0f) * 0.7f
        else -> 0f
    }

    val effectiveLipColor = (lipColor + makeupLipOffset + 
            (uiOnlyFeatures["mouthSize"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["philtrum"] ?: 0f) * 0.2f + 
            (uiOnlyFeatures["augmentation"] ?: 0f) * 0.3f + 
            (uiOnlyFeatures["mouth3D"] ?: 0f) * 0.2f).coerceIn(0f, 1f)

    val makeupToneOffset = when {
        (uiOnlyFeatures["makeupDaily"] ?: 0f) > 0.01f -> 0.1f
        else -> 0f
    }
    val effectiveSkinTone = (skinTone + makeupToneOffset + 
            (uiOnlyFeatures["whiten"] ?: 0f) * 0.5f).coerceIn(0f, 1f)

    val effectiveBrightness = (brightness - 
            (uiOnlyFeatures["oiliness"] ?: 0f) * 0.15f).coerceIn(-0.5f, 0.5f)

    // Makeup Filter modifications
    val effectiveFilterType = when {
        (uiOnlyFeatures["makeupPinky"] ?: 0f) > 0.01f -> 4 // Pink filter
        (uiOnlyFeatures["makeupPeach"] ?: 0f) > 0.01f -> 1 // Warm filter
        else -> filterType
    }
    
    val effectiveFilterIntensity = when {
        (uiOnlyFeatures["makeupPinky"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupPinky"] ?: 0f) * 0.4f
        (uiOnlyFeatures["makeupPeach"] ?: 0f) > 0.01f -> (uiOnlyFeatures["makeupPeach"] ?: 0f) * 0.5f
        else -> filterIntensity
    }

    // Save UI-only features when updated
    LaunchedEffect(uiOnlyFeatures.toMap()) {
        sharedPrefs.edit().apply {
            uiOnlyFeatures.forEach { (key, value) ->
                putFloat(key, value)
            }
            apply()
        }
    }

    // Handle beauty updates in the renderer using effective values
    LaunchedEffect(effectiveSmoothing) { cameraView?.setSmoothing(effectiveSmoothing) }
    LaunchedEffect(effectiveBrightness) { cameraView?.setBrightness(effectiveBrightness) }
    LaunchedEffect(effectiveSkinTone) { cameraView?.setSkinTone(effectiveSkinTone) }
    LaunchedEffect(effectiveEyeEnlargement) { cameraView?.setEyeEnlargement(effectiveEyeEnlargement) }
    LaunchedEffect(effectiveFaceSlimming) { cameraView?.setFaceSlimming(effectiveFaceSlimming) }
    LaunchedEffect(effectiveDarkCircleRemover) { cameraView?.setDarkCircleRemover(effectiveDarkCircleRemover) }
    LaunchedEffect(effectiveJawSharpening) { cameraView?.setJawSharpening(effectiveJawSharpening) }
    LaunchedEffect(effectiveNoseSlimming) { cameraView?.setNoseSlimming(effectiveNoseSlimming) }
    LaunchedEffect(effectiveLipColor) { cameraView?.setLipColor(effectiveLipColor) }
    LaunchedEffect(effectiveFilterType, effectiveFilterIntensity) { cameraView?.setFilter(effectiveFilterType, effectiveFilterIntensity) }
    
    LaunchedEffect(smoothing, brightness, skinTone, eyeEnlargement, faceSlimming,
        darkCircleRemover, jawSharpening, noseSlimming, lipColor,
        filterType, filterIntensity) {
        onBeautySettingsChanged(smoothing, brightness, skinTone, eyeEnlargement, faceSlimming,
            darkCircleRemover, jawSharpening, noseSlimming, lipColor,
            filterType, filterIntensity)
        val params = android.os.Bundle().apply {
            putFloat("smoothing", smoothing)
            putFloat("brightness", brightness)
            putFloat("skinTone", skinTone)
            putFloat("eyeEnlargement", eyeEnlargement)
            putFloat("faceSlimming", faceSlimming)
            putFloat("darkCircleRemover", darkCircleRemover)
            putFloat("jawSharpening", jawSharpening)
            putFloat("noseSlimming", noseSlimming)
            putFloat("lipColor", lipColor)
            putInt("filterType", filterType)
            putFloat("filterIntensity", filterIntensity)
        }
        com.tejaslabs.beauty.ai.camera.firebase.FirebaseManager.logEvent("beauty_settings_changed", params)
    }
    LaunchedEffect(flashMode, timerSeconds, isGridEnabled, activeAspectRatio) {
        onCameraSettingsChanged(flashMode, timerSeconds, isGridEnabled, activeAspectRatio)
    }
    LaunchedEffect(isFrontCamera) { cameraView?.setCameraFacing(isFrontCamera) }
    LaunchedEffect(flashMode) { cameraView?.setFlashMode(flashMode) }

    // Selected feature tracking for slider display
    var selectedFeatureId by remember { mutableStateOf<String?>(null) }

    // Beauty Tab feature items list
    val beautyFeatures = remember(smoothing, brightness, skinTone, eyeEnlargement, faceSlimming, darkCircleRemover, noseSlimming, lipColor, uiOnlyFeatures) {
        listOf(
            FeatureItem("skin", "Skin", FeatureIcon.Emoji("👤"), type = "presets_skin", getValue = { smoothing }, onValueChange = { smoothing = it }),
            FeatureItem("face", "Face", FeatureIcon.Emoji("😊"), type = "presets_face", getValue = { faceSlimming }, onValueChange = { faceSlimming = it }),
            FeatureItem("slim", "Slim", FeatureIcon.Emoji("↔"), getValue = { faceSlimming }, onValueChange = { faceSlimming = it }),
            FeatureItem("eyeSize", "Eye Size", FeatureIcon.Vector(Icons.Default.RemoveRedEye), getValue = { eyeEnlargement }, onValueChange = { eyeEnlargement = it }),
            FeatureItem("noseSize", "Nose Size", FeatureIcon.Emoji("👃"), getValue = { noseSlimming }, onValueChange = { noseSlimming = it }),
            FeatureItem("tone", "Tone", FeatureIcon.Vector(Icons.Default.ColorLens), getValue = { skinTone }, onValueChange = { skinTone = it }),
            FeatureItem("headSize", "Head Size", FeatureIcon.Emoji("🗣"), getValue = { uiOnlyFeatures["headSize"] ?: 0f }, onValueChange = { uiOnlyFeatures["headSize"] = it }),
            FeatureItem("acne", "Acne", FeatureIcon.Emoji("⚫"), getValue = { uiOnlyFeatures["acne"] ?: 0f }, onValueChange = { uiOnlyFeatures["acne"] = it }),
            FeatureItem("smileLines", "Smile Lines", FeatureIcon.Emoji("😊"), getValue = { uiOnlyFeatures["smileLines"] ?: 0f }, onValueChange = { uiOnlyFeatures["smileLines"] = it }),
            FeatureItem("darkCircles", "Dark Circles", FeatureIcon.Emoji("👁‍🗨"), getValue = { darkCircleRemover }, onValueChange = { darkCircleRemover = it }),
            FeatureItem("brighten", "Brighten", FeatureIcon.Emoji("☀️"), min = -0.5f, max = 0.5f, getValue = { brightness }, onValueChange = { brightness = it }),
            FeatureItem("oiliness", "Oiliness", FeatureIcon.Emoji("💧"), getValue = { uiOnlyFeatures["oiliness"] ?: 0f }, onValueChange = { uiOnlyFeatures["oiliness"] = it }),
            FeatureItem("whiten", "Whiten", FeatureIcon.Emoji("🦷"), getValue = { uiOnlyFeatures["whiten"] ?: 0f }, onValueChange = { uiOnlyFeatures["whiten"] = it }),
            FeatureItem("softHair", "Soft Hair", FeatureIcon.Emoji("💇"), getValue = { uiOnlyFeatures["softHair"] ?: 0f }, onValueChange = { uiOnlyFeatures["softHair"] = it })
        )
    }

    // Advanced Sub-tabs feature items list
    val advancedFaceFeatures = remember(faceSlimming, jawSharpening, uiOnlyFeatures) {
        listOf(
            FeatureItem("width", "Width", FeatureIcon.Emoji("↔"), getValue = { faceSlimming }, onValueChange = { faceSlimming = it }),
            FeatureItem("faceLength", "Length", FeatureIcon.Emoji("↕"), getValue = { uiOnlyFeatures["faceLength"] ?: 0f }, onValueChange = { uiOnlyFeatures["faceLength"] = it }),
            FeatureItem("faceMiddle", "Middle", FeatureIcon.Emoji("⊙"), getValue = { uiOnlyFeatures["faceMiddle"] ?: 0f }, onValueChange = { uiOnlyFeatures["faceMiddle"] = it }),
            FeatureItem("faceTop", "Top", FeatureIcon.Emoji("⊤"), getValue = { uiOnlyFeatures["faceTop"] ?: 0f }, onValueChange = { uiOnlyFeatures["faceTop"] = it }),
            FeatureItem("foreheadSmooth", "3D Forehead", FeatureIcon.Emoji("🔴"), getValue = { uiOnlyFeatures["foreheadSmooth"] ?: 0f }, onValueChange = { uiOnlyFeatures["foreheadSmooth"] = it }),
            FeatureItem("faceLift", "3D Facelift", FeatureIcon.Emoji("🔴"), getValue = { uiOnlyFeatures["faceLift"] ?: 0f }, onValueChange = { uiOnlyFeatures["faceLift"] = it }),
            FeatureItem("cheek", "3D Cheeks", FeatureIcon.Emoji("😊"), getValue = { uiOnlyFeatures["cheek"] ?: 0f }, onValueChange = { uiOnlyFeatures["cheek"] = it }),
            FeatureItem("sculpting", "Sculpting", FeatureIcon.Emoji("⊡"), getValue = { uiOnlyFeatures["sculpting"] ?: 0f }, onValueChange = { uiOnlyFeatures["sculpting"] = it }),
            FeatureItem("jawline", "Jawline", FeatureIcon.Emoji("⊏"), getValue = { jawSharpening }, onValueChange = { jawSharpening = it }),
            FeatureItem("mandible", "Mandible", FeatureIcon.Emoji("⊐"), getValue = { uiOnlyFeatures["mandible"] ?: 0f }, onValueChange = { uiOnlyFeatures["mandible"] = it }),
            FeatureItem("chin", "Chin", FeatureIcon.Emoji("⊑"), getValue = { uiOnlyFeatures["chin"] ?: 0f }, onValueChange = { uiOnlyFeatures["chin"] = it }),
            FeatureItem("jaw", "Jaw", FeatureIcon.Emoji("⊒"), getValue = { uiOnlyFeatures["jaw"] ?: 0f }, onValueChange = { uiOnlyFeatures["jaw"] = it })
        )
    }

    val advancedEyesFeatures = remember(eyeEnlargement, uiOnlyFeatures) {
        listOf(
            FeatureItem("eyeSizeAdv", "Eye Size", FeatureIcon.Vector(Icons.Default.RemoveRedEye), getValue = { eyeEnlargement }, onValueChange = { eyeEnlargement = it }),
            FeatureItem("eyeAngle", "Eye Angle", FeatureIcon.Emoji("∠"), getValue = { uiOnlyFeatures["eyeAngle"] ?: 0f }, onValueChange = { uiOnlyFeatures["eyeAngle"] = it }),
            FeatureItem("eyeDistance", "Eye Distance", FeatureIcon.Emoji("⊗"), getValue = { uiOnlyFeatures["eyeDistance"] ?: 0f }, onValueChange = { uiOnlyFeatures["eyeDistance"] = it })
        )
    }

    val advancedNoseFeatures = remember(noseSlimming, uiOnlyFeatures) {
        listOf(
            FeatureItem("nose3D", "3D Nose", FeatureIcon.Emoji("🔴"), getValue = { uiOnlyFeatures["nose3D"] ?: 0f }, onValueChange = { uiOnlyFeatures["nose3D"] = it }),
            FeatureItem("bridge", "Bridge", FeatureIcon.Emoji("⊏"), getValue = { noseSlimming }, onValueChange = { noseSlimming = it }),
            FeatureItem("noseTip", "Tip", FeatureIcon.Emoji("⊐"), getValue = { uiOnlyFeatures["noseTip"] ?: 0f }, onValueChange = { uiOnlyFeatures["noseTip"] = it })
        )
    }

    val advancedMouthFeatures = remember(lipColor, uiOnlyFeatures) {
        listOf(
            FeatureItem("mouthSize", "Mouth Size", FeatureIcon.Emoji("👄"), getValue = { uiOnlyFeatures["mouthSize"] ?: 0f }, onValueChange = { uiOnlyFeatures["mouthSize"] = it }),
            FeatureItem("philtrum", "Philtrum", FeatureIcon.Emoji("⊡"), getValue = { uiOnlyFeatures["philtrum"] ?: 0f }, onValueChange = { uiOnlyFeatures["philtrum"] = it }),
            FeatureItem("augmentation", "Augmentation", FeatureIcon.Emoji("⊕"), getValue = { uiOnlyFeatures["augmentation"] ?: 0f }, onValueChange = { uiOnlyFeatures["augmentation"] = it }),
            FeatureItem("mouth3D", "3D Correction", FeatureIcon.Emoji("🔴"), getValue = { uiOnlyFeatures["mouth3D"] ?: 0f }, onValueChange = { uiOnlyFeatures["mouth3D"] = it })
        )
    }

    fun setMakeupPreset(key: String, value: Float) {
        val makeupKeys = listOf("makeupNone", "makeupDaily", "makeupPinky", "makeupPeach", "makeupRedLips")
        makeupKeys.forEach { k ->
            uiOnlyFeatures[k] = if (k == key) value else 0f
        }
    }

    // Makeup Tab feature items list
    val makeupFeatures = remember(uiOnlyFeatures) {
        listOf(
            FeatureItem("makeupNone", "None", FeatureIcon.Emoji("❌"), getValue = { uiOnlyFeatures["makeupNone"] ?: 0f }, onValueChange = { setMakeupPreset("makeupNone", it) }),
            FeatureItem("makeupDaily", "Daily", FeatureIcon.Emoji("💄"), getValue = { uiOnlyFeatures["makeupDaily"] ?: 0f }, onValueChange = { setMakeupPreset("makeupDaily", it) }),
            FeatureItem("makeupPinky", "Pinky", FeatureIcon.Emoji("🌸"), getValue = { uiOnlyFeatures["makeupPinky"] ?: 0f }, onValueChange = { setMakeupPreset("makeupPinky", it) }),
            FeatureItem("makeupPeach", "Peach", FeatureIcon.Emoji("🍑"), getValue = { uiOnlyFeatures["makeupPeach"] ?: 0f }, onValueChange = { setMakeupPreset("makeupPeach", it) }),
            FeatureItem("makeupRedLips", "Red Lips", FeatureIcon.Emoji("💋"), getValue = { uiOnlyFeatures["makeupRedLips"] ?: 0f }, onValueChange = { setMakeupPreset("makeupRedLips", it) })
        )
    }

    val allFeatures = beautyFeatures + advancedFaceFeatures + advancedEyesFeatures + advancedNoseFeatures + advancedMouthFeatures + makeupFeatures
    val selectedFeature = allFeatures.find { it.id == selectedFeatureId }

    // Helpers to identify selected presets
    val currentSkinPreset = when (smoothing) {
        0.0f -> "None"
        0.3f -> "Classic"
        0.5f -> "Glass"
        0.7f -> "HD"
        0.9f -> "Matte"
        else -> ""
    }

    val currentFacePreset = when (faceSlimming) {
        0.0f -> "None"
        0.2f -> "Classic"
        0.4f -> "Basic"
        0.6f -> "Petite"
        0.8f -> "Delicate"
        else -> ""
    }

    fun resetAllSettings() {
        smoothing = 0.0f
        brightness = 0.0f
        skinTone = 0.0f
        eyeEnlargement = 0.0f
        faceSlimming = 0.0f
        darkCircleRemover = 0.0f
        jawSharpening = 0.0f
        noseSlimming = 0.0f
        lipColor = 0.0f
        filterType = 0
        filterIntensity = 0.60f
        
        uiOnlyFeatures.keys.forEach { key ->
            uiOnlyFeatures[key] = 0.0f
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // 1. Live Viewfinder Aspect Ratio Container
        Box(
            modifier = when (activeAspectRatio) {
                "1:1" -> Modifier.fillMaxWidth().aspectRatio(1f).align(Alignment.Center)
                "3:4" -> Modifier.fillMaxWidth().aspectRatio(3f / 4f).align(Alignment.Center)
                else -> Modifier.fillMaxSize() // 16:9 fills the full screen
            }
        ) {
            // Live Camera Preview
            AndroidView(
                factory = { ctx ->
                    CameraGLView(ctx).apply {
                        bindToLifecycle(lifecycleOwner)
                        setCameraFacing(isFrontCamera)
                        setSmoothing(effectiveSmoothing)
                        setBrightness(effectiveBrightness)
                        setSkinTone(effectiveSkinTone)
                        setEyeEnlargement(effectiveEyeEnlargement)
                        setFaceSlimming(effectiveFaceSlimming)
                        setDarkCircleRemover(effectiveDarkCircleRemover)
                        setJawSharpening(effectiveJawSharpening)
                        setNoseSlimming(effectiveNoseSlimming)
                        setLipColor(effectiveLipColor)
                        setFilter(effectiveFilterType, effectiveFilterIntensity)
                        setFlashMode(flashMode)
                    }.also { cameraView = it }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> }
            )

            // Rule of Thirds Grid Overlay
            if (isGridEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeColor = Color.White.copy(alpha = 0.4f)
                    val strokeWidth = 1.dp.toPx()
                    
                    // Vertical lines
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width / 3f, 0f),
                        end = Offset(size.width / 3f, size.height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width * 2f / 3f, 0f),
                        end = Offset(size.width * 2f / 3f, size.height),
                        strokeWidth = strokeWidth
                    )
                    
                    // Horizontal lines
                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, size.height / 3f),
                        end = Offset(size.width, size.height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, size.height * 2f / 3f),
                        end = Offset(size.width, size.height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }

        // 2. Active Timer Badge overlay (bottom right of preview)
        if (timerSeconds > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = if (isPanelVisible || showFilterPanel) 260.dp else 128.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Active Timer",
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${timerSeconds}s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Top Translucent Control Bar (Mockup Redesign)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close / Exit button (left)
            IconButton(onClick = {
                // Exit app or go back, currently no-op or close panel
                if (isPanelVisible || showFilterPanel) {
                    isPanelVisible = false
                    showFilterPanel = false
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Aspect Ratio Selector
            Text(
                text = activeAspectRatio,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clickable { showAspectRatioDialog = true }
            )

            // Filter Circle Toggle (replaces old bottom sheet)
            IconButton(onClick = {
                showFilterPanel = !showFilterPanel
                if (showFilterPanel) {
                    isPanelVisible = false // hide beauty drawer
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Filters",
                    tint = if (showFilterPanel) Color(0xFFFF4081) else Color.White
                )
            }

            // More Options (...) trigger
            IconButton(onClick = { showMoreOptionsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.White
                )
            }

            // Flip Camera
            IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = "Flip Camera",
                    tint = Color.White
                )
            }
        }

        // 4. Large Timer Countdown Overlay
        AnimatedVisibility(
            visible = isCountingDown && countdownRemaining > 0,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(100)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownRemaining",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Slider Overlay (White thumb, thin track, floating above bottom sheet)
        if ((isPanelVisible || showFilterPanel) && selectedFeature != null && selectedFeature.type == "slider") {
            val currentValue = selectedFeature.getValue()
            val sliderMin = selectedFeature.min
            val sliderMax = selectedFeature.max
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 315.dp) // Float just above the bottom sheet (height 300dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedFeature.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Slider(
                        value = currentValue,
                        onValueChange = { selectedFeature.onValueChange(it) },
                        valueRange = sliderMin..sliderMax,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    val sliderDisplayValue = if (sliderMin < 0) {
                        ((currentValue - sliderMin) / (sliderMax - sliderMin) * 200 - 100).toInt()
                    } else {
                        (currentValue * 100).toInt()
                    }
                    
                    Text(
                        text = "$sliderDisplayValue",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Small close button to deselect the feature
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Deselect",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { selectedFeatureId = null }
                    )
                }
            }
        }

        // 6. Redesigned Bottom Panel Drawer (Light/White theme)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            
            if (showFilterPanel) {
                // Filter Panel Drawer (White themed)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.White)
                        .padding(top = 12.dp)
                ) {
                    // Filter Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filters",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showFilterPanel = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Filter Intensity Slider (inside panel, white background, pink slider)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Intensity",
                            color = Color(0xFF495057),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value = filterIntensity,
                            onValueChange = { filterIntensity = it },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF4081),
                                activeTrackColor = Color(0xFFFF4081),
                                inactiveTrackColor = Color(0xFFDEE2E6)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(filterIntensity * 100).toInt()}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Horizontally scrollable list of filter icons
                    val filtersList = listOf(
                        "Natural", "Warm", "Cool", "Vintage", "Pink",
                        "Film", "Bright", "Soft", "Mono", "Sunset",
                        "Glow", "Fresh", "Dream", "Matte", "Classic"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        filtersList.forEachIndexed { index, name ->
                            FilterThumb(
                                name = name,
                                isSelected = filterType == index,
                                lightTheme = true,
                                onClick = { filterType = index }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Integrated Bottom Bar (Remove / Capture / Reset)
                    IntegratedBottomBar(
                        onRemove = {
                            filterType = 0
                            filterIntensity = 0f
                        },
                        onCapture = {
                            if (!isCountingDown) {
                                if (timerSeconds > 0) {
                                    countdownRemaining = timerSeconds
                                    isCountingDown = true
                                } else {
                                    triggerCapture(
                                        cameraView = cameraView,
                                        aspectRatio = activeAspectRatio,
                                        smoothing = effectiveSmoothing,
                                        brightness = effectiveBrightness,
                                        skinTone = effectiveSkinTone,
                                        eye = effectiveEyeEnlargement,
                                        face = effectiveFaceSlimming,
                                        darkCircle = effectiveDarkCircleRemover,
                                        jaw = effectiveJawSharpening,
                                        nose = effectiveNoseSlimming,
                                        lip = effectiveLipColor,
                                        filterType = effectiveFilterType,
                                        filterIntensity = effectiveFilterIntensity,
                                        onPhotoCaptured = onPhotoCaptured
                                    )
                                }
                            }
                        },
                        onReset = {
                            filterType = 0
                            filterIntensity = 0.6f
                        },
                        latestPhotoUri = latestPhotoUri,
                        onOpenGallery = onOpenGallery
                    )
                }
            } else if (isPanelVisible) {
                // Redesigned Beauty Bottom Panel (White themed)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.White)
                        .padding(top = 8.dp)
                ) {
                    // Panel Header: Tabs and Close Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Main Tabs row: Beauty | Advanced | Makeup
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf("Beauty", "Advanced", "Makeup")
                            tabs.forEach { tab ->
                                val isSelected = activeTab == tab
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        activeTab = tab
                                        selectedFeatureId = null
                                    }
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (isSelected) Color.Black else Color(0xFF868E96),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                    )
                                    // Underline
                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .width(28.dp)
                                            .background(if (isSelected) Color(0xFFFF4081) else Color.Transparent)
                                    )
                                }
                            }
                        }
                        
                        // Close button (X)
                        IconButton(
                            onClick = { isPanelVisible = false },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close panel",
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF1F3F5))
                    )
                    
                    // Advanced sub-tabs row (only if Advanced is selected)
                    if (activeTab == "Advanced") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val subTabs = listOf("Face", "Eyes", "Nose", "Mouth")
                            subTabs.forEach { subTab ->
                                val isSelected = activeAdvancedSubTab == subTab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFFFF0F5) else Color.Transparent)
                                        .clickable {
                                            activeAdvancedSubTab = subTab
                                            selectedFeatureId = null
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = subTab,
                                        color = if (isSelected) Color(0xFFFF4081) else Color(0xFF495057),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Presets container row (only shown if a preset category is active)
                    if (activeTab == "Beauty" && selectedFeatureId == "skin") {
                        val skinPresets = listOf("None", "Classic", "Glass", "HD", "Matte")
                        val skinPresetValues = listOf(0.0f, 0.3f, 0.5f, 0.7f, 0.9f)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Presets:",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            skinPresets.forEachIndexed { i, preset ->
                                PresetCapsule(
                                    name = preset,
                                    isSelected = currentSkinPreset == preset,
                                    onClick = {
                                        smoothing = skinPresetValues[i]
                                    }
                                )
                            }
                        }
                    } else if (activeTab == "Beauty" && selectedFeatureId == "face") {
                        val facePresets = listOf("None", "Classic", "Basic", "Petite", "Delicate")
                        val facePresetValues = listOf(0.0f, 0.2f, 0.4f, 0.6f, 0.8f)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Presets:",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            facePresets.forEachIndexed { i, preset ->
                                PresetCapsule(
                                    name = preset,
                                    isSelected = currentFacePreset == preset,
                                    onClick = {
                                        faceSlimming = facePresetValues[i]
                                    }
                                )
                            }
                        }
                    }
                    
                    // Horizontal scroll feature icons row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentFeaturesList = when (activeTab) {
                            "Beauty" -> beautyFeatures
                            "Advanced" -> {
                                when (activeAdvancedSubTab) {
                                    "Face" -> advancedFaceFeatures
                                    "Eyes" -> advancedEyesFeatures
                                    "Nose" -> advancedNoseFeatures
                                    else -> advancedMouthFeatures
                                }
                            }
                            else -> makeupFeatures
                        }
                        
                        currentFeaturesList.forEach { feature ->
                            val isSelected = selectedFeatureId == feature.id
                            val currentValue = feature.getValue()
                            
                            // A feature is "active" (dot shown) if its current value is non-zero (or offset from baseline)
                            val isActive = if (feature.min < 0) {
                                Math.abs(currentValue - 0.0f) > 0.01f
                            } else {
                                currentValue > 0.01f
                            }
                            
                            BeautyIconItem(
                                label = feature.label,
                                icon = feature.icon,
                                isSelected = isSelected,
                                isActive = isActive,
                                onClick = {
                                    selectedFeatureId = feature.id
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Integrated Bottom Bar (Remove / Capture / Reset)
                    IntegratedBottomBar(
                        onRemove = {
                            selectedFeature?.let { feature ->
                                feature.onValueChange(0.0f)
                            }
                        },
                        onCapture = {
                            if (!isCountingDown) {
                                if (timerSeconds > 0) {
                                    countdownRemaining = timerSeconds
                                    isCountingDown = true
                                } else {
                                    triggerCapture(
                                        cameraView = cameraView,
                                        aspectRatio = activeAspectRatio,
                                        smoothing = effectiveSmoothing,
                                        brightness = effectiveBrightness,
                                        skinTone = effectiveSkinTone,
                                        eye = effectiveEyeEnlargement,
                                        face = effectiveFaceSlimming,
                                        darkCircle = effectiveDarkCircleRemover,
                                        jaw = effectiveJawSharpening,
                                        nose = effectiveNoseSlimming,
                                        lip = effectiveLipColor,
                                        filterType = effectiveFilterType,
                                        filterIntensity = effectiveFilterIntensity,
                                        onPhotoCaptured = onPhotoCaptured
                                    )
                                }
                            }
                        },
                        onReset = {
                            resetAllSettings()
                        },
                        latestPhotoUri = latestPhotoUri,
                        onOpenGallery = onOpenGallery
                    )
                }
            } else {
                // Closed Drawer (Original layout with translucent trigger buttons)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(bottom = 24.dp)
                ) {
                    // Trigger buttons row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Beauty",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    activeTab = "Beauty"
                                    isPanelVisible = true
                                    showFilterPanel = false
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Filter",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    showFilterPanel = true
                                    isPanelVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Makeup",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    activeTab = "Makeup"
                                    isPanelVisible = true
                                    showFilterPanel = false
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Capture row when drawer is closed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery shortcut
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                .clickable { onOpenGallery() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (latestPhotoUri != null) {
                                AsyncImage(
                                    model = latestPhotoUri,
                                    contentDescription = "Gallery",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = Color.White
                                )
                            }
                        }

                        // Pink Capture Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4081))
                                .clickable {
                                    if (!isCountingDown) {
                                        if (timerSeconds > 0) {
                                            countdownRemaining = timerSeconds
                                            isCountingDown = true
                                        } else {
                                            triggerCapture(
                                                cameraView = cameraView,
                                                aspectRatio = activeAspectRatio,
                                                smoothing = effectiveSmoothing,
                                                brightness = effectiveBrightness,
                                                skinTone = effectiveSkinTone,
                                                eye = effectiveEyeEnlargement,
                                                face = effectiveFaceSlimming,
                                                darkCircle = effectiveDarkCircleRemover,
                                                jaw = effectiveJawSharpening,
                                                nose = effectiveNoseSlimming,
                                                lip = effectiveLipColor,
                                                filterType = effectiveFilterType,
                                                filterIntensity = effectiveFilterIntensity,
                                                onPhotoCaptured = onPhotoCaptured
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Camera Flip Switch
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Main countdown timer effect
    if (isCountingDown && countdownRemaining > 0) {
        LaunchedEffect(countdownRemaining) {
            kotlinx.coroutines.delay(1000)
            countdownRemaining -= 1
            if (countdownRemaining == 0) {
                isCountingDown = false
                triggerCapture(
                    cameraView = cameraView,
                    aspectRatio = activeAspectRatio,
                    smoothing = effectiveSmoothing,
                    brightness = effectiveBrightness,
                    skinTone = effectiveSkinTone,
                    eye = effectiveEyeEnlargement,
                    face = effectiveFaceSlimming,
                    darkCircle = effectiveDarkCircleRemover,
                    jaw = effectiveJawSharpening,
                    nose = effectiveNoseSlimming,
                    lip = effectiveLipColor,
                    filterType = effectiveFilterType,
                    filterIntensity = effectiveFilterIntensity,
                    onPhotoCaptured = onPhotoCaptured
                )
            }
        }
    }

    // Dialog: Timer Options Selection
    if (showTimerDialog) {
        Dialog(onDismissRequest = { showTimerDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Countdown Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(0, 3, 5, 10)
                    options.forEach { sec ->
                        val text = if (sec == 0) "Off" else "$sec seconds"
                        Text(
                            text = text,
                            color = if (timerSeconds == sec) Color(0xFFFF4081) else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (timerSeconds == sec) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    timerSeconds = sec
                                    showTimerDialog = false
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog: Aspect Ratio Selector Dialog
    if (showAspectRatioDialog) {
        Dialog(onDismissRequest = { showAspectRatioDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Aspect Ratio",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val options = listOf("3:4", "1:1", "16:9")
                    var tempSelection by remember { mutableStateOf(activeAspectRatio) }
                    
                    options.forEach { ratio ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelection = ratio }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = tempSelection == ratio,
                                onClick = { tempSelection = ratio },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF4081))
                            )
                            Text(
                                text = ratio,
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { showAspectRatioDialog = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Confirm",
                            color = Color(0xFFFF4081),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    activeAspectRatio = tempSelection
                                    showAspectRatioDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialog: More Options Dialog (Flash, Grid, Timer selection unified)
    if (showMoreOptionsDialog) {
        Dialog(onDismissRequest = { showMoreOptionsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Camera Settings",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Flash option row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Flash Mode", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val flashModesList = listOf(
                                ImageCapture.FLASH_MODE_OFF to "Off",
                                ImageCapture.FLASH_MODE_ON to "On",
                                ImageCapture.FLASH_MODE_AUTO to "Auto"
                            )
                            flashModesList.forEach { (mode, label) ->
                                val selected = flashMode == mode
                                Text(
                                    text = label,
                                    color = if (selected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { flashMode = mode }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Grid Toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Overlay", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Show", false to "Hide").forEach { (gridVal, label) ->
                                val selected = isGridEnabled == gridVal
                                Text(
                                    text = label,
                                    color = if (selected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { isGridEnabled = gridVal }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Timer option row shortcut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Timer", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(
                            text = if (timerSeconds == 0) "Off" else "${timerSeconds}s",
                            color = Color(0xFFFF4081),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    showMoreOptionsDialog = false
                                    showTimerDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Close",
                        color = Color(0xFFFF4081),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMoreOptionsDialog = false }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IntegratedBottomBar(
    onRemove: () -> Unit,
    onCapture: () -> Unit,
    onReset: () -> Unit,
    latestPhotoUri: Uri?,
    onOpenGallery: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Remove (slash-circle icon)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(60.dp)
                .clickable { onRemove() }
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Remove effect",
                tint = Color(0xFF495057),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Remove",
                color = Color(0xFF495057),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Center: Capture Button (Pink camera button)
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(4.dp, Color(0xFFE9ECEF), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4081))
                .clickable { onCapture() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // Right: Reset Button (Circular arrow icon)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(60.dp)
                .clickable { onReset() }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset all",
                tint = Color(0xFF495057),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reset",
                color = Color(0xFF495057),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun triggerCapture(
    cameraView: CameraGLView?,
    aspectRatio: String,
    smoothing: Float,
    brightness: Float,
    skinTone: Float,
    eye: Float,
    face: Float,
    darkCircle: Float,
    jaw: Float,
    nose: Float,
    lip: Float,
    filterType: Int,
    filterIntensity: Float,
    onPhotoCaptured: (File, String, Float, Float, Float, Float, Float, Float, Float, Float, Float, Int, Float) -> Unit
) {
    val params = android.os.Bundle().apply {
        putString("aspect_ratio", aspectRatio)
        putFloat("smoothing", smoothing)
        putFloat("brightness", brightness)
        putFloat("skin_tone", skinTone)
        putFloat("eye_enlargement", eye)
        putFloat("face_slimming", face)
        putFloat("dark_circle_remover", darkCircle)
        putFloat("jaw_sharpening", jaw)
        putFloat("nose_slimming", nose)
        putFloat("lip_color", lip)
        putInt("filter_type", filterType)
        putFloat("filter_intensity", filterIntensity)
    }
    com.tejaslabs.beauty.ai.camera.firebase.FirebaseManager.logEvent("photo_captured", params)

    cameraView?.capturePhoto(
        onPhotoCaptured = { file ->
            onPhotoCaptured(file, aspectRatio, smoothing, brightness, skinTone, eye, face, darkCircle, jaw, nose, lip, filterType, filterIntensity)
        },
        onError = { err ->
            Log.e("CameraScreen", "Capture error", err)
            com.tejaslabs.beauty.ai.camera.firebase.FirebaseManager.recordException(err)
        }
    )
}
