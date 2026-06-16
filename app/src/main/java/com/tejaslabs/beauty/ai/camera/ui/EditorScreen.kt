package com.tejaslabs.beauty.ai.camera.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.tejaslabs.beauty.ai.camera.editor.ImageEditorProcessor
import com.tejaslabs.beauty.ai.camera.detector.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditorScreen(
    imageFile: File?,
    imageUri: Uri?,
    initialSmoothing: Float,
    initialBrightness: Float,
    initialSkinTone: Float,
    initialEyeEnlargement: Float,
    initialFaceSlimming: Float,
    initialFilterType: Int,
    initialFilterIntensity: Float,
    initialCropRatio: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
 
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
 
    // Undo/Redo stacks
    val history = remember { mutableStateListOf<Bitmap>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
 
    // Load original bitmap and apply initial aspect ratio crop and beauty filters
    LaunchedEffect(key1 = true) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val bitmap = loadBitmapWithRotation(context, imageFile, imageUri)
            if (bitmap != null) {
                // Pre-crop image based on selected aspect ratio from camera
                val croppedBmp = when (initialCropRatio) {
                    "1:1" -> cropToCenterRatio(bitmap, 1.0f)
                    "3:4" -> cropToCenterRatio(bitmap, 0.75f)
                    "16:9" -> cropToCenterRatio(bitmap, 9f / 16f)
                    else -> bitmap
                }

                // If this is a captured photo, apply camera beauty settings (smoothing, whitening, eye, face)
                val beautifiedBmp = if (imageFile != null) {
                    val landmarks = FaceDetector.detectStatic(context, croppedBmp)

                    ImageEditorProcessor.applyCameraBeauty(
                        bitmap = croppedBmp,
                        smoothing = initialSmoothing,
                        skinTone = initialSkinTone,
                        eyeEnlargement = initialEyeEnlargement,
                        faceSlimming = initialFaceSlimming,
                        landmarks = landmarks
                    )
                } else {
                    croppedBmp
                }

                // Pre-apply camera filter and brightness
                val initialProcessed = ImageEditorProcessor.applyEdits(
                    bitmap = beautifiedBmp,
                    brightness = initialBrightness,
                    contrast = 0f,
                    saturation = 0f,
                    filterType = initialFilterType,
                    filterIntensity = initialFilterIntensity
                )
                
                withContext(Dispatchers.Main) {
                    originalBitmap = beautifiedBmp
                    currentBitmap = initialProcessed
                    history.clear()
                    history.add(initialProcessed)
                    historyIndex = 0
                    isLoading = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    // Helper to push new bitmap to undo/redo history
    val pushToHistory: (Bitmap) -> Unit = { newBitmap ->
        while (history.size > historyIndex + 1) {
            history.removeLast()
        }
        history.add(newBitmap)
        historyIndex = history.size - 1
        currentBitmap = newBitmap
    }

    // Control Tabs
    var activeTool by remember { mutableStateOf("Adjust") } // Adjust, Filter, Crop, Rotate
    
    // Sliders & settings states
    var brightness by remember { mutableFloatStateOf(0f) } // -100 to 100
    var contrast by remember { mutableFloatStateOf(0f) }   // -100 to 100
    var saturation by remember { mutableFloatStateOf(0f) } // -100 to 100
    
    var activeFilter by remember { mutableIntStateOf(0) }
    var filterIntensity by remember { mutableFloatStateOf(0.60f) }
    
    var cropRatio by remember { mutableStateOf(initialCropRatio) } // Free, 1:1, 4:5, 3:4, 16:9
    var activeAdjustment by remember { mutableStateOf("Brightness") } // Brightness, Contrast, Saturation

    var cropBoxX by remember { mutableFloatStateOf(0.1f) }
    var cropBoxY by remember { mutableFloatStateOf(0.1f) }
    var cropBoxW by remember { mutableFloatStateOf(0.8f) }
    var cropBoxH by remember { mutableFloatStateOf(0.8f) }
    var dragHandle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cropRatio, currentBitmap) {
        val bmp = currentBitmap ?: return@LaunchedEffect
        val imgAspect = bmp.width.toFloat() / bmp.height.toFloat()
        val targetAspect = when (cropRatio) {
            "1:1" -> 1.0f
            "4:5" -> 0.8f
            "3:4" -> 0.75f
            "16:9" -> 9f / 16f
            else -> 0f // Free
        }

        if (targetAspect > 0f) {
            if (imgAspect > targetAspect) {
                cropBoxH = 0.8f
                cropBoxW = cropBoxH * (targetAspect / imgAspect)
            } else {
                cropBoxW = 0.8f
                cropBoxH = cropBoxW * (imgAspect / targetAspect)
            }
        } else {
            cropBoxW = 0.8f
            cropBoxH = 0.8f
        }
        
        cropBoxX = (1f - cropBoxW) / 2f
        cropBoxY = (1f - cropBoxH) / 2f
    }

    // Dialog sheets
    var showSaveDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            showSaveDialog -> showSaveDialog = false
            activeTool != "Adjust" -> activeTool = "Adjust"
            else -> onBack()
        }
    }

    // Live preview processing effect
    LaunchedEffect(key1 = true) {
        var lastAppliedBrightness = -999f
        var lastAppliedContrast = -999f
        var lastAppliedSaturation = -999f
        var lastAppliedFilter = -1
        var lastAppliedFilterIntensity = -1f
        var lastAppliedHistoryIndex = -1

        while (isActive) {
            if (brightness != lastAppliedBrightness ||
                contrast != lastAppliedContrast ||
                saturation != lastAppliedSaturation ||
                activeFilter != lastAppliedFilter ||
                filterIntensity != lastAppliedFilterIntensity ||
                historyIndex != lastAppliedHistoryIndex
            ) {
                val targetBrightness = brightness
                val targetContrast = contrast
                val targetSaturation = saturation
                val targetFilter = activeFilter
                val targetFilterIntensity = filterIntensity
                val targetHistoryIndex = historyIndex

                val baseBmp = if (targetHistoryIndex >= 0 && targetHistoryIndex < history.size) {
                    history[targetHistoryIndex]
                } else {
                    originalBitmap
                }

                if (baseBmp != null) {
                    val processed = withContext(Dispatchers.Default) {
                        ImageEditorProcessor.applyEdits(
                            bitmap = baseBmp,
                            brightness = targetBrightness,
                            contrast = targetContrast,
                            saturation = targetSaturation,
                            filterType = targetFilter,
                            filterIntensity = targetFilterIntensity
                        )
                    }
                    currentBitmap = processed
                }

                lastAppliedBrightness = targetBrightness
                lastAppliedContrast = targetContrast
                lastAppliedSaturation = targetSaturation
                lastAppliedFilter = targetFilter
                lastAppliedFilterIntensity = targetFilterIntensity
                lastAppliedHistoryIndex = targetHistoryIndex
            }
            delay(33) // ~30 FPS limit for smooth slider dragging preview updates
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(text = "Editor", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            }

            Row {
                IconButton(
                    onClick = {
                        if (historyIndex > 0) {
                            historyIndex--
                            currentBitmap = history[historyIndex]
                            // Reset sliders to avoid applying offsets on top of the old state
                            brightness = 0f
                            contrast = 0f
                            saturation = 0f
                            activeFilter = 0
                        }
                    },
                    enabled = historyIndex > 0
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = if (historyIndex > 0) Color.White else Color.Gray)
                }
                
                IconButton(
                    onClick = {
                        if (historyIndex < history.size - 1) {
                            historyIndex++
                            currentBitmap = history[historyIndex]
                            brightness = 0f
                            contrast = 0f
                            saturation = 0f
                            activeFilter = 0
                        }
                    },
                    enabled = historyIndex < history.size - 1
                ) {
                    Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = if (historyIndex < history.size - 1) Color.White else Color.Gray)
                }

                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Save Options", tint = Color(0xFFFF4081))
                }
            }
        }

        // 2. Viewfinder Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFFF4081))
            } else {
                currentBitmap?.let { bmp ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                .pointerInput(activeTool, cropRatio) {
                                    if (activeTool != "Crop") return@pointerInput
                                    
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val pxX = cropBoxX * canvasWidth
                                            val pxY = cropBoxY * canvasHeight
                                            val pxW = cropBoxW * canvasWidth
                                            val pxH = cropBoxH * canvasHeight
                                            val touchThresh = 40.dp.toPx()
                                            
                                            val dxTL = offset.x - pxX
                                            val dyTL = offset.y - pxY
                                            val distTL = dxTL * dxTL + dyTL * dyTL
                                            
                                            val dxTR = offset.x - (pxX + pxW)
                                            val dyTR = offset.y - pxY
                                            val distTR = dxTR * dxTR + dyTR * dyTR
                                            
                                            val dxBL = offset.x - pxX
                                            val dyBL = offset.y - (pxY + pxH)
                                            val distBL = dxBL * dxBL + dyBL * dyBL
                                            
                                            val dxBR = offset.x - (pxX + pxW)
                                            val dyBR = offset.y - (pxY + pxH)
                                            val distBR = dxBR * dxBR + dyBR * dyBR
                                            
                                            val limit = touchThresh * touchThresh
                                            
                                            dragHandle = when {
                                                distTL < limit -> "TL"
                                                distTR < limit -> "TR"
                                                distBL < limit -> "BL"
                                                distBR < limit -> "BR"
                                                offset.x >= pxX && offset.x <= pxX + pxW && offset.y >= pxY && offset.y <= pxY + pxH -> "MOVE"
                                                else -> null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (dragHandle == null) return@detectDragGestures
                                            
                                            val targetAspect = when (cropRatio) {
                                                "1:1" -> 1.0f
                                                "4:5" -> 0.8f
                                                "3:4" -> 0.75f
                                                "16:9" -> 9f / 16f
                                                else -> 0f
                                            }
                                            
                                            val rightX = cropBoxX + cropBoxW
                                            val bottomY = cropBoxY + cropBoxH
                                            
                                            if (targetAspect > 0f) {
                                                val K = targetAspect / (canvasWidth / canvasHeight)
                                                
                                                when (dragHandle) {
                                                    "MOVE" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        cropBoxX = (cropBoxX + deltaX).coerceIn(0f, 1f - cropBoxW)
                                                        cropBoxY = (cropBoxY + deltaY).coerceIn(0f, 1f - cropBoxH)
                                                    }
                                                    "BR" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        var newW = (cropBoxW + deltaX).coerceIn(0.1f, 1f - cropBoxX)
                                                        var newH = newW / K
                                                        if (newH > 1f - cropBoxY) {
                                                            newH = 1f - cropBoxY
                                                            newW = newH * K
                                                        }
                                                        cropBoxW = newW
                                                        cropBoxH = newH
                                                    }
                                                    "TL" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        var newX = (cropBoxX + deltaX).coerceIn(0f, rightX - 0.1f)
                                                        var newW = rightX - newX
                                                        var newH = newW / K
                                                        var newY = bottomY - newH
                                                        if (newY < 0f) {
                                                            newY = 0f
                                                            newH = bottomY
                                                            newW = newH * K
                                                            newX = rightX - newW
                                                        }
                                                        if (newX >= 0f && newY >= 0f) {
                                                            cropBoxX = newX
                                                            cropBoxY = newY
                                                            cropBoxW = newW
                                                            cropBoxH = newH
                                                        }
                                                    }
                                                    "TR" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        var newW = (cropBoxW + deltaX).coerceIn(0.1f, 1f - cropBoxX)
                                                        var newH = newW / K
                                                        var newY = bottomY - newH
                                                        if (newY < 0f) {
                                                            newY = 0f
                                                            newH = bottomY
                                                            newW = newH * K
                                                        }
                                                        cropBoxW = newW
                                                        cropBoxY = newY
                                                        cropBoxH = newH
                                                    }
                                                    "BL" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        var newX = (cropBoxX + deltaX).coerceIn(0f, rightX - 0.1f)
                                                        var newW = rightX - newX
                                                        var newH = newW / K
                                                        if (newH > 1f - cropBoxY) {
                                                            newH = 1f - cropBoxY
                                                            newW = newH * K
                                                            newX = rightX - newW
                                                        }
                                                        cropBoxX = newX
                                                        cropBoxW = newW
                                                        cropBoxH = newH
                                                    }
                                                }
                                            } else {
                                                // Free crop (no aspect ratio constraint)
                                                when (dragHandle) {
                                                    "MOVE" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        cropBoxX = (cropBoxX + deltaX).coerceIn(0f, 1f - cropBoxW)
                                                        cropBoxY = (cropBoxY + deltaY).coerceIn(0f, 1f - cropBoxH)
                                                    }
                                                    "BR" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        cropBoxW = (cropBoxW + deltaX).coerceIn(0.1f, 1f - cropBoxX)
                                                        cropBoxH = (cropBoxH + deltaY).coerceIn(0.1f, 1f - cropBoxY)
                                                    }
                                                    "TL" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        val newX = (cropBoxX + deltaX).coerceIn(0f, rightX - 0.1f)
                                                        val newY = (cropBoxY + deltaY).coerceIn(0f, bottomY - 0.1f)
                                                        cropBoxX = newX
                                                        cropBoxY = newY
                                                        cropBoxW = rightX - newX
                                                        cropBoxH = bottomY - newY
                                                    }
                                                    "TR" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        val newY = (cropBoxY + deltaY).coerceIn(0f, bottomY - 0.1f)
                                                        cropBoxW = (cropBoxW + deltaX).coerceIn(0.1f, 1f - cropBoxX)
                                                        cropBoxY = newY
                                                        cropBoxH = bottomY - newY
                                                    }
                                                    "BL" -> {
                                                        val deltaX = dragAmount.x / canvasWidth
                                                        val deltaY = dragAmount.y / canvasHeight
                                                        val newX = (cropBoxX + deltaX).coerceIn(0f, rightX - 0.1f)
                                                        cropBoxX = newX
                                                        cropBoxW = rightX - newX
                                                        cropBoxH = (cropBoxH + deltaY).coerceIn(0.1f, 1f - cropBoxY)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = { dragHandle = null },
                                        onDragCancel = { dragHandle = null }
                                    )
                                }
                        ) {
                            drawImage(image = bmp.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                            
                            // Draw Crop Grid Overlay if Crop tool is selected
                            if (activeTool == "Crop") {
                                val strokeWidth = 1.dp.toPx()
                                val gridColor = Color.White.copy(alpha = 0.4f)
                                
                                val startX = cropBoxX * size.width
                                val startY = cropBoxY * size.height
                                val cropW = cropBoxW * size.width
                                val cropH = cropBoxH * size.height
                                
                                // Draw bounding box with solid white color
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(startX, startY),
                                    size = Size(cropW, cropH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                                )
                                
                                // Draw corner handles
                                val handleLength = 16.dp.toPx()
                                val handleThickness = 4.dp.toPx()
                                val capHandleColor = Color.White
                                
                                // TL Corner
                                drawRect(capHandleColor, topLeft = Offset(startX - handleThickness/2f, startY - handleThickness/2f), size = Size(handleLength, handleThickness))
                                drawRect(capHandleColor, topLeft = Offset(startX - handleThickness/2f, startY - handleThickness/2f), size = Size(handleThickness, handleLength))
                                
                                // TR Corner
                                drawRect(capHandleColor, topLeft = Offset(startX + cropW - handleLength + handleThickness/2f, startY - handleThickness/2f), size = Size(handleLength, handleThickness))
                                drawRect(capHandleColor, topLeft = Offset(startX + cropW - handleThickness/2f, startY - handleThickness/2f), size = Size(handleThickness, handleLength))
                                
                                // BL Corner
                                drawRect(capHandleColor, topLeft = Offset(startX - handleThickness/2f, startY + cropH - handleThickness/2f), size = Size(handleLength, handleThickness))
                                drawRect(capHandleColor, topLeft = Offset(startX - handleThickness/2f, startY + cropH - handleLength + handleThickness/2f), size = Size(handleThickness, handleLength))
                                
                                // BR Corner
                                drawRect(capHandleColor, topLeft = Offset(startX + cropW - handleLength + handleThickness/2f, startY + cropH - handleThickness/2f), size = Size(handleLength, handleThickness))
                                drawRect(capHandleColor, topLeft = Offset(startX + cropW - handleThickness/2f, startY + cropH - handleLength + handleThickness/2f), size = Size(handleThickness, handleLength))

                                // Draw 3x3 grid inside crop box
                                drawLine(gridColor, Offset(startX + cropW / 3f, startY), Offset(startX + cropW / 3f, startY + cropH), strokeWidth)
                                drawLine(gridColor, Offset(startX + cropW * 2f / 3f, startY), Offset(startX + cropW * 2f / 3f, startY + cropH), strokeWidth)
                                drawLine(gridColor, Offset(startX, startY + cropH / 3f), Offset(startX + cropW, startY + cropH / 3f), strokeWidth)
                                drawLine(gridColor, Offset(startX, startY + cropH * 2f / 3f), Offset(startX + cropW, startY + cropH * 2f / 3f), strokeWidth)
                            }
                        }
                    }
                }
            }
        }

        // 3. Tool adjustment Sliders/Selections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161616))
                .padding(vertical = 12.dp)
        ) {
            when (activeTool) {
                "Adjust" -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Brightness", "Contrast", "Saturation").forEach { name ->
                                Text(
                                    text = name,
                                    color = if (activeAdjustment == name) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (activeAdjustment == name) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { activeAdjustment = name }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Adjustment Slider
                        val sliderVal = when (activeAdjustment) {
                            "Brightness" -> brightness
                            "Contrast" -> contrast
                            else -> saturation
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-100",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier.width(32.dp)
                            )
                            Slider(
                                value = sliderVal,
                                onValueChange = {
                                    when (activeAdjustment) {
                                        "Brightness" -> brightness = it
                                        "Contrast" -> contrast = it
                                        "Saturation" -> saturation = it
                                    }
                                },
                                valueRange = -100f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF4081),
                                    activeTrackColor = Color(0xFFFF4081)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "100",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Apply / Cancel Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                // Reset sliders, triggering LaunchedEffect to revert currentBitmap
                                brightness = 0f
                                contrast = 0f
                                saturation = 0f
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(onClick = {
                                // Commit the live adjusted bitmap to history
                                currentBitmap?.let { bmp ->
                                    pushToHistory(bmp)
                                    // Reset sliders so the next edit starts from 0 relative offset
                                    brightness = 0f
                                    contrast = 0f
                                    saturation = 0f
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Apply", tint = Color(0xFFFF4081))
                            }
                        }
                    }
                }
                "Filter" -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(text = "Filter Intensity", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = filterIntensity,
                                onValueChange = { filterIntensity = it },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF4081), activeTrackColor = Color(0xFFFF4081)),
                                modifier = Modifier.weight(1f)
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
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            filtersList.forEachIndexed { index, name ->
                                FilterThumb(
                                    name = name,
                                    isSelected = activeFilter == index,
                                    onClick = {
                                        activeFilter = index
                                    }
                                )
                            }
                        }

                        // Apply / Cancel Row for Filters
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                activeFilter = 0
                                filterIntensity = 0.60f
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(onClick = {
                                currentBitmap?.let { bmp ->
                                    pushToHistory(bmp)
                                    activeFilter = 0
                                    filterIntensity = 0.60f
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Apply", tint = Color(0xFFFF4081))
                            }
                        }
                    }
                }
                "Crop" -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            listOf("Free", "1:1", "4:5", "3:4", "16:9").forEach { ratio ->
                                Text(
                                    text = ratio,
                                    color = if (cropRatio == ratio) Color(0xFFFF4081) else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (cropRatio == ratio) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable { cropRatio = ratio }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { activeTool = "Adjust" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(onClick = {
                                val baseBmp = if (historyIndex >= 0 && historyIndex < history.size) {
                                    history[historyIndex]
                                } else {
                                    originalBitmap
                                }

                                baseBmp?.let { bmp ->
                                    val width = bmp.width
                                    val height = bmp.height
                                    
                                    val cropX = (cropBoxX * width).toInt().coerceIn(0, width - 1)
                                    val cropY = (cropBoxY * height).toInt().coerceIn(0, height - 1)
                                    val cropW = (cropBoxW * width).toInt().coerceIn(10, width - cropX)
                                    val cropH = (cropBoxH * height).toInt().coerceIn(10, height - cropY)
                                    
                                    try {
                                        val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                                        pushToHistory(cropped)
                                    } catch (e: Exception) {
                                        Log.e("EditorScreen", "Cropping failed", e)
                                    }
                                    activeTool = "Adjust"
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Apply", tint = Color(0xFFFF4081))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Primary Tool Navigator Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.Black),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolItem(
                    label = "Adjust",
                    icon = Icons.Default.Tune,
                    isSelected = activeTool == "Adjust",
                    onClick = { activeTool = "Adjust" }
                )
                ToolItem(
                    label = "Filter",
                    icon = Icons.Default.FilterBAndW,
                    isSelected = activeTool == "Filter",
                    onClick = { activeTool = "Filter" }
                )
                ToolItem(
                    label = "Crop",
                    icon = Icons.Default.Crop,
                    isSelected = activeTool == "Crop",
                    onClick = { activeTool = "Crop" }
                )
                ToolItem(
                    label = "Rotate",
                    icon = Icons.Default.RotateRight,
                    isSelected = activeTool == "Rotate",
                    onClick = {
                        // Apply rotation on top of current history index and push directly
                        val baseBmp = if (historyIndex >= 0 && historyIndex < history.size) {
                            history[historyIndex]
                        } else {
                            originalBitmap
                        }
                        
                        baseBmp?.let { bmp ->
                            val rotated = ImageEditorProcessor.rotateBitmap(bmp, 90f)
                            pushToHistory(rotated)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Save & Share Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Bypasses custom menu, opens the Android generic system share sheet directly
                        currentBitmap?.let { bmp ->
                            sharePhotoToPackage(context, bmp, "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share", fontSize = 14.sp)
                }

                Button(
                    onClick = { showSaveDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4081),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Save Photo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // A. Save Options Dialog Sheet (Format and Quality Selection)
    if (showSaveDialog) {
        var saveOption by remember { mutableStateOf("copy") } // copy or overwrite
        var formatOption by remember { mutableStateOf("JPEG") } // JPEG, PNG, WEBP
        var qualityOption by remember { mutableStateOf("High") } // High, Medium, Low

        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Save Photo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Option: Save as Copy
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveOption = "copy" }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = saveOption == "copy",
                            onClick = { saveOption = "copy" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF4081))
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Save as Copy", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Save a new photo to gallery", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }

                    // Option: Overwrite Original
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveOption = "overwrite" }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = saveOption == "overwrite",
                            onClick = { saveOption = "overwrite" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF4081))
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Overwrite Original", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Replace the original photo", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector: Format
                    Text(text = "Format", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("JPEG", "PNG", "WEBP").forEach { fmt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (formatOption == fmt) Color(0xFFFF4081) else Color(0xFF2E2E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { formatOption = fmt }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = fmt, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector: Quality (relevant for JPEG & WEBP)
                    if (formatOption != "PNG") {
                        Text(text = "Quality", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("High", "Medium", "Low").forEach { q ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (qualityOption == q) Color(0xFFFF4081) else Color(0xFF2E2E2E),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { qualityOption = q }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = q, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showSaveDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel")
                        }

                        Button(
                            onClick = {
                                showSaveDialog = false
                                currentBitmap?.let { bmp ->
                                    val saved = savePhotoToMediaStore(
                                        context = context,
                                        bitmap = bmp,
                                        qualityStr = qualityOption,
                                        formatStr = formatOption
                                    )
                                    if (saved) {
                                        Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081), contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Crop helper function
private fun cropToCenterRatio(bmp: Bitmap, targetAspect: Float): Bitmap {
    val width = bmp.width
    val height = bmp.height
    val currentAspect = width.toFloat() / height.toFloat()
    
    return if (currentAspect > targetAspect) {
        val newW = (height * targetAspect).toInt()
        val startX = (width - newW) / 2
        Bitmap.createBitmap(bmp, startX, 0, newW, height)
    } else {
        val newH = (width / targetAspect).toInt()
        val startY = (height - newH) / 2
        Bitmap.createBitmap(bmp, 0, startY, width, newH)
    }
}

// Helper to save bitmap into MediaStore with compression format and quality settings
private fun savePhotoToMediaStore(context: Context, bitmap: Bitmap, qualityStr: String, formatStr: String): Boolean {
    val resolver = context.contentResolver
    
    val format = when (formatStr) {
        "PNG" -> Bitmap.CompressFormat.PNG
        "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        else -> Bitmap.CompressFormat.JPEG
    }

    val mimeType = when (formatStr) {
        "PNG" -> "image/png"
        "WEBP" -> "image/webp"
        else -> "image/jpeg"
    }

    val extension = when (formatStr) {
        "PNG" -> ".png"
        "WEBP" -> ".webp"
        else -> ".jpg"
    }

    val quality = when (qualityStr) {
        "Medium" -> 80
        "Low" -> 50
        else -> 95 // High
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Beauty_${System.currentTimeMillis()}$extension")
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/BeautyCamera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

    return try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(format, quality, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        Log.e("EditorScreen", "Error saving image to mediastore", e)
        resolver.delete(uri, null, null)
        false
    }
}

// Helper to share photo through intent using system chooser
private fun sharePhotoToPackage(context: Context, bitmap: Bitmap, packageName: String) {
    try {
        val cacheFile = File(context.cacheDir, "share_temp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, cacheFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (packageName.isNotEmpty()) {
                setPackage(packageName)
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
    } catch (e: Exception) {
        Log.e("EditorScreen", "Failed to share image", e)
        Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
    }
}

private fun loadBitmapWithRotation(context: Context, imageFile: File?, imageUri: Uri?): Bitmap? {
    try {
        val bitmap = when {
            imageFile != null -> BitmapFactory.decodeFile(imageFile.absolutePath)
            imageUri != null -> {
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            else -> null
        } ?: return null

        val orientation = when {
            imageFile != null -> {
                val exif = android.media.ExifInterface(imageFile.absolutePath)
                exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
            }
            imageUri != null -> {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    val exif = android.media.ExifInterface(stream)
                    exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                } ?: android.media.ExifInterface.ORIENTATION_NORMAL
            }
            else -> android.media.ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = android.graphics.Matrix()
        var rotate = 0f
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270f
        }

        return if (rotate != 0f) {
            matrix.postRotate(rotate)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e("EditorScreen", "Error loading bitmap with rotation", e)
        return null
    }
}

