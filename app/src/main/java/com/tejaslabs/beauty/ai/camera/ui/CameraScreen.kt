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
 
    // Control States
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
    var activeTab by remember { mutableStateOf("Beauty") } // Beauty or Filter
    var isPanelVisible by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = isPanelVisible || showTimerDialog || showAspectRatioDialog) {
        when {
            showTimerDialog -> showTimerDialog = false
            showAspectRatioDialog -> showAspectRatioDialog = false
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
 
    // Handle beauty updates in the renderer
    LaunchedEffect(smoothing) { cameraView?.setSmoothing(smoothing) }
    LaunchedEffect(brightness) { cameraView?.setBrightness(brightness) }
    LaunchedEffect(skinTone) { cameraView?.setSkinTone(skinTone) }
    LaunchedEffect(eyeEnlargement) { cameraView?.setEyeEnlargement(eyeEnlargement) }
    LaunchedEffect(faceSlimming) { cameraView?.setFaceSlimming(faceSlimming) }
    LaunchedEffect(darkCircleRemover) { cameraView?.setDarkCircleRemover(darkCircleRemover) }
    LaunchedEffect(jawSharpening) { cameraView?.setJawSharpening(jawSharpening) }
    LaunchedEffect(noseSlimming) { cameraView?.setNoseSlimming(noseSlimming) }
    LaunchedEffect(lipColor) { cameraView?.setLipColor(lipColor) }
    LaunchedEffect(filterType, filterIntensity) { cameraView?.setFilter(filterType, filterIntensity) }
    
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
                        setSmoothing(smoothing)
                        setBrightness(brightness)
                        setSkinTone(skinTone)
                        setEyeEnlargement(eyeEnlargement)
                        setFaceSlimming(faceSlimming)
                        setDarkCircleRemover(darkCircleRemover)
                        setJawSharpening(jawSharpening)
                        setNoseSlimming(noseSlimming)
                        setLipColor(lipColor)
                        setFilter(filterType, filterIntensity)
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
                    .padding(bottom = if (isPanelVisible) 340.dp else 128.dp, end = 16.dp)
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

        // 3. Top Translucent Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect Ratio selector button
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

            // Flash Toggle
            val flashIcon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                else -> Icons.Default.FlashOff
            }
            IconButton(onClick = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            }) {
                Icon(imageVector = flashIcon, contentDescription = "Flash", tint = Color.White)
            }

            // Timer Selection trigger
            IconButton(onClick = { showTimerDialog = true }) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timerSeconds > 0) Color(0xFFFF4081) else Color.White
                    )
                    if (timerSeconds > 0) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(0xFFFF4081), CircleShape)
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$timerSeconds",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }

            // Grid toggle
            IconButton(onClick = { isGridEnabled = !isGridEnabled }) {
                Icon(
                    imageVector = if (isGridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                    contentDescription = "Grid",
                    tint = if (isGridEnabled) Color(0xFFFF4081) else Color.White
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

        // 5. Bottom Control Drawer (Sliders, Tabs, Capture controls)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(bottom = 24.dp)
        ) {
            
            // A. Sliding control panel for Active Tab (with Close Button)
            AnimatedVisibility(
                visible = isPanelVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Panel Close Button (X)
                    IconButton(
                        onClick = { isPanelVisible = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Panel",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    if (activeTab == "Beauty") {
                        // Scrollable sliders list with all beauty options
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(end = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            BeautySliderRow(label = "Skin Smoothing", value = smoothing, onValueChange = { smoothing = it })
                            BeautySliderRow(label = "Brightness", value = brightness, min = -0.5f, max = 0.5f, onValueChange = { brightness = it })
                            BeautySliderRow(label = "Skin Tone", value = skinTone, onValueChange = { skinTone = it })
                            BeautySliderRow(label = "Eye Enlargement", value = eyeEnlargement, onValueChange = { eyeEnlargement = it })
                            BeautySliderRow(label = "Face Slimming", value = faceSlimming, onValueChange = { faceSlimming = it })
                            BeautySliderRow(label = "Dark Circle Remover", value = darkCircleRemover, onValueChange = { darkCircleRemover = it })
                            BeautySliderRow(label = "Jaw Sharpening", value = jawSharpening, onValueChange = { jawSharpening = it })
                            BeautySliderRow(label = "Nose Slimming", value = noseSlimming, onValueChange = { noseSlimming = it })
                            BeautySliderRow(label = "Lip Color", value = lipColor, onValueChange = { lipColor = it })
                        }
                    } else {
                        // Horizontal scroll filters list with intensity slider
                        Column(modifier = Modifier.padding(end = 24.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Intensity",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Slider(
                                    value = filterIntensity,
                                    onValueChange = { filterIntensity = it },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFF4081),
                                        activeTrackColor = Color(0xFFFF4081),
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(filterIntensity * 100).toInt()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            val filtersList = listOf(
                                "Natural", "Warm", "Cool", "Vintage", "Pink",
                                "Film", "Bright", "Soft", "Mono", "Sunset",
                                "Glow", "Fresh", "Dream", "Matte", "Classic"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                filtersList.forEachIndexed { index, name ->
                                    FilterThumb(
                                        name = name,
                                        isSelected = filterType == index,
                                        onClick = { filterType = index }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B. Tab switcher (reopens panel on tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Black),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Beauty",
                    color = if (activeTab == "Beauty" && isPanelVisible) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable {
                            activeTab = "Beauty"
                            isPanelVisible = true
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Filter",
                    color = if (activeTab == "Filter" && isPanelVisible) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable {
                            activeTab = "Filter"
                            isPanelVisible = true
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. Bottom Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                // Gallery shortcut thumbnail
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
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                    }
                }

                // Capture Button
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
                                        smoothing = smoothing,
                                        brightness = brightness,
                                        skinTone = skinTone,
                                        eye = eyeEnlargement,
                                        face = faceSlimming,
                                        darkCircle = darkCircleRemover,
                                        jaw = jawSharpening,
                                        nose = noseSlimming,
                                        lip = lipColor,
                                        filterType = filterType,
                                        filterIntensity = filterIntensity,
                                        onPhotoCaptured = onPhotoCaptured
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Camera flip switch
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Cached, contentDescription = "Flip Camera", tint = Color.White, modifier = Modifier.size(24.dp))
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
                    smoothing = smoothing,
                    brightness = brightness,
                    skinTone = skinTone,
                    eye = eyeEnlargement,
                    face = faceSlimming,
                    darkCircle = darkCircleRemover,
                    jaw = jawSharpening,
                    nose = noseSlimming,
                    lip = lipColor,
                    filterType = filterType,
                    filterIntensity = filterIntensity,
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

    // Dialog: Aspect Ratio Selector Dialog with confirmation buttons
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
