package com.tejaslabs.beauty.ai.camera.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.tejaslabs.beauty.ai.camera.detector.FaceLandmarksData

object ImageEditorProcessor {

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropBitmap(bitmap: Bitmap, leftPercent: Float, topPercent: Float, rightPercent: Float, bottomPercent: Float): Bitmap {
        val left = (leftPercent * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (topPercent * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = ((rightPercent - leftPercent) * bitmap.width).toInt().coerceIn(1, bitmap.width - left)
        val height = ((bottomPercent - topPercent) * bitmap.height).toInt().coerceIn(1, bitmap.height - top)
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun applyEdits(
        bitmap: Bitmap,
        // Adjustments
        brightness: Float, // -100 to 100
        contrast: Float,   // -100 to 100
        saturation: Float, // -100 to 100
        // Filters
        filterType: Int,
        filterIntensity: Float // 0 to 1
    ): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

        val combinedMatrix = ColorMatrix()

        // 1. Apply Contrast Matrix
        val contrastScale = (contrast / 100f) + 1.0f // 0 to 2
        val contrastTranslate = (-0.5f * contrastScale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrastScale, 0f, 0f, 0f, contrastTranslate,
            0f, contrastScale, 0f, 0f, contrastTranslate,
            0f, 0f, contrastScale, 0f, contrastTranslate,
            0f, 0f, 0f, 1f, 0f
        ))
        combinedMatrix.postConcat(contrastMatrix)

        // 2. Apply Saturation Matrix
        val saturationScale = (saturation / 100f) + 1.0f // 0 to 2
        val saturationMatrix = ColorMatrix().apply { setSaturation(saturationScale) }
        combinedMatrix.postConcat(saturationMatrix)

        // 3. Apply Brightness Matrix
        val brightnessOffset = (brightness / 100f) * 80f // map to -80 to 80
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightnessOffset,
            0f, 1f, 0f, 0f, brightnessOffset,
            0f, 0f, 1f, 0f, brightnessOffset,
            0f, 0f, 0f, 1f, 0f
        ))
        combinedMatrix.postConcat(brightnessMatrix)

        // 4. Apply Filter Matrix (blended based on filterIntensity)
        if (filterType >= 0 && filterIntensity > 0f) {
            val filterMatrix = getFilterColorMatrix(filterType)
            
            // Perform linear interpolation between Identity matrix and filter matrix
            val blendedMatrix = ColorMatrix()
            val identity = ColorMatrix().array
            val filterArr = filterMatrix.array
            val blendedArr = blendedMatrix.array
            
            for (i in 0 until 20) {
                blendedArr[i] = identity[i] + (filterArr[i] - identity[i]) * filterIntensity
            }
            
            combinedMatrix.postConcat(blendedMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun getFilterColorMatrix(type: Int): ColorMatrix {
        return when (type) {
            1 -> { // Warm
                ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 0f,
                    0f, 1.06f, 0f, 0f, 0f,
                    0f, 0f, 0.92f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            2 -> { // Cool
                ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0f, 0f, 0f,
                    0f, 1.02f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            3 -> { // Vintage (Sepia)
                ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            4 -> { // Pink
                ColorMatrix(floatArrayOf(
                    1.16f, 0f, 0f, 0f, 0f,
                    0f, 0.94f, 0f, 0f, 0f,
                    0f, 0f, 1.06f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            5 -> { // Film (Faded, lift blacks)
                ColorMatrix(floatArrayOf(
                    1.10f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 6f,
                    0f, 0f, 1.05f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            6 -> { // Bright
                ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 0f,
                    0f, 1.15f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            7 -> { // Soft (lowered contrast)
                ColorMatrix(floatArrayOf(
                    0.88f, 0f, 0f, 0f, 15f,
                    0f, 0.88f, 0f, 0f, 15f,
                    0f, 0f, 0.88f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            8 -> { // Black & White (Grayscale)
                ColorMatrix().apply { setSaturation(0f) }
            }
            9 -> { // Sunset
                ColorMatrix(floatArrayOf(
                    1.18f, 0f, 0f, 0f, 0f,
                    0f, 0.96f, 0f, 0f, 0f,
                    0f, 0f, 0.82f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            10 -> { // Glow
                ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 25f,
                    0f, 1.05f, 0f, 0f, 25f,
                    0f, 0f, 1.05f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            11 -> { // Fresh
                ColorMatrix(floatArrayOf(
                    0.96f, 0f, 0f, 0f, 0f,
                    0f, 1.12f, 0f, 0f, 0f,
                    0f, 0f, 1.04f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            12 -> { // Dream (slightly faded warm pinkish)
                ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 12f,
                    0f, 0.96f, 0f, 0f, 6f,
                    0f, 0f, 1.02f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            13 -> { // Matte
                ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0f, 0f, 12f,
                    0f, 0.92f, 0f, 0f, 12f,
                    0f, 0f, 0.92f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            14 -> { // Classic
                ColorMatrix(floatArrayOf(
                    1.20f, 0f, 0f, 0f, -10f,
                    0f, 1.20f, 0f, 0f, -10f,
                    0f, 0f, 1.20f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> ColorMatrix() // Original / Natural (0)
        }
    }

    fun applyCameraBeauty(
        bitmap: Bitmap,
        smoothing: Float,
        skinTone: Float,
        eyeEnlargement: Float,
        faceSlimming: Float,
        landmarks: FaceLandmarksData?
    ): Bitmap {
        var processed = bitmap
        
        // 1. Warp eyes and slim face if landmarks are available
        if (landmarks != null) {
            processed = applyWarp(
                bitmap = processed,
                leftEyeX = landmarks.leftEyeX,
                leftEyeY = landmarks.leftEyeY,
                rightEyeX = landmarks.rightEyeX,
                rightEyeY = landmarks.rightEyeY,
                noseX = landmarks.noseX,
                noseY = landmarks.noseY,
                eyeEnlargement = eyeEnlargement,
                faceSlimming = faceSlimming
            )
        }

        // 2. Apply skin smoothing and whitening
        processed = applySkinEffects(
            bitmap = processed,
            smoothing = smoothing,
            skinTone = skinTone,
            landmarks = landmarks
        )

        return processed
    }

    private fun applyWarp(
        bitmap: Bitmap,
        leftEyeX: Float, leftEyeY: Float,
        rightEyeX: Float, rightEyeY: Float,
        noseX: Float, noseY: Float,
        eyeEnlargement: Float,
        faceSlimming: Float
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val maxDim = Math.max(width, height).toFloat()
        val eyeRadius = 0.05f * maxDim
        val faceRadius = 0.15f * maxDim

        // Warp eyes
        if (eyeEnlargement > 0.01f) {
            warpRegionBulge(bitmap, output, leftEyeX * width, leftEyeY * height, eyeRadius, eyeEnlargement)
            warpRegionBulge(bitmap, output, rightEyeX * width, rightEyeY * height, eyeRadius, eyeEnlargement)
        }

        // Warp face slimming
        if (faceSlimming > 0.01f) {
            warpRegionSlim(bitmap, output, noseX * width, noseY * height, faceRadius, faceSlimming)
        }

        return output
    }

    private fun warpRegionBulge(
        src: Bitmap,
        dest: Bitmap,
        centerX: Float,
        centerY: Float,
        radius: Float,
        intensity: Float
    ) {
        val startX = (centerX - radius).toInt().coerceIn(0, src.width - 1)
        val endX = (centerX + radius).toInt().coerceIn(0, src.width - 1)
        val startY = (centerY - radius).toInt().coerceIn(0, src.height - 1)
        val endY = (centerY + radius).toInt().coerceIn(0, src.height - 1)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val dx = x - centerX
                val dy = y - centerY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist < radius) {
                    val percent = dist / radius
                    val strength = 1.0f - (1.0f - percent) * (1.0f - percent) * intensity * 0.35f
                    val srcX = centerX + dx * strength
                    val srcY = centerY + dy * strength
                    dest.setPixel(x, y, getBilinearPixel(src, srcX, srcY))
                }
            }
        }
    }

    private fun warpRegionSlim(
        src: Bitmap,
        dest: Bitmap,
        centerX: Float,
        centerY: Float,
        radius: Float,
        intensity: Float
    ) {
        val startX = (centerX - radius).toInt().coerceIn(0, src.width - 1)
        val endX = (centerX + radius).toInt().coerceIn(0, src.width - 1)
        val startY = (centerY - 0.02f * src.height).toInt().coerceIn(0, src.height - 1)
        val endY = (centerY + radius).toInt().coerceIn(0, src.height - 1)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val dx = x - centerX
                val dy = y - centerY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist < radius) {
                    val percent = dist / radius
                    val strength = (1.0f - percent) * (1.0f - percent) * intensity * 0.15f
                    val srcX = centerX + dx * (1.0f + strength)
                    val srcY = y.toFloat()
                    dest.setPixel(x, y, getBilinearPixel(src, srcX, srcY))
                }
            }
        }
    }

    private fun getBilinearPixel(bitmap: Bitmap, x: Float, y: Float): Int {
        val x1 = x.toInt().coerceIn(0, bitmap.width - 1)
        val y1 = y.toInt().coerceIn(0, bitmap.height - 1)
        val x2 = (x1 + 1).coerceIn(0, bitmap.width - 1)
        val y2 = (y1 + 1).coerceIn(0, bitmap.height - 1)
        
        val dx = x - x1
        val dy = y - y1
        
        val c11 = bitmap.getPixel(x1, y1)
        val c21 = bitmap.getPixel(x2, y1)
        val c12 = bitmap.getPixel(x1, y2)
        val c22 = bitmap.getPixel(x2, y2)
        
        val r = interpolateChannel(c11 shr 16 and 0xff, c21 shr 16 and 0xff, c12 shr 16 and 0xff, c22 shr 16 and 0xff, dx, dy)
        val g = interpolateChannel(c11 shr 8 and 0xff, c21 shr 8 and 0xff, c12 shr 8 and 0xff, c22 shr 8 and 0xff, dx, dy)
        val b = interpolateChannel(c11 and 0xff, c21 and 0xff, c12 and 0xff, c22 and 0xff, dx, dy)
        val a = interpolateChannel(c11 ushr 24, c21 ushr 24, c12 ushr 24, c22 ushr 24, dx, dy)
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun interpolateChannel(c11: Int, c21: Int, c12: Int, c22: Int, dx: Float, dy: Float): Int {
        val val1 = c11 * (1 - dx) + c21 * dx
        val val2 = c12 * (1 - dx) + c22 * dx
        return (val1 * (1 - dy) + val2 * dy).toInt().coerceIn(0, 255)
    }

    private fun applySkinEffects(
        bitmap: Bitmap,
        smoothing: Float,
        skinTone: Float,
        landmarks: FaceLandmarksData?
    ): Bitmap {
        if (smoothing < 0.05f && skinTone < 0.05f) {
            return bitmap
        }
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val skinToneTargetR = 255f
        val skinToneTargetG = 240f
        val skinToneTargetB = 240f
        val exponent = 1.0f - skinTone * 0.25f

        val powerTable = FloatArray(256) { i ->
            Math.pow(i / 255.0, exponent.toDouble()).toFloat() * 255f
        }

        // Face bounding box details if landmarks are found
        var faceLeft = 0
        var faceRight = width - 1
        var faceTop = 0
        var faceBottom = height - 1

        if (landmarks != null) {
            val minX = (Math.min(Math.min(landmarks.leftEyeX, landmarks.rightEyeX), landmarks.noseX) - 0.15f) * width
            val maxX = (Math.max(Math.max(landmarks.leftEyeX, landmarks.rightEyeX), landmarks.noseX) + 0.15f) * width
            val minY = (Math.min(Math.min(landmarks.leftEyeY, landmarks.rightEyeY), landmarks.noseY) - 0.20f) * height
            val maxY = (Math.max(Math.max(landmarks.leftEyeY, landmarks.rightEyeY), landmarks.noseY) + 0.25f) * height
            
            faceLeft = minX.toInt().coerceIn(0, width - 1)
            faceRight = maxX.toInt().coerceIn(0, width - 1)
            faceTop = minY.toInt().coerceIn(0, height - 1)
            faceBottom = maxY.toInt().coerceIn(0, height - 1)
        }

        val outPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val c = pixels[idx]
                val a = c ushr 24
                var r = (c shr 16 and 0xff).toFloat()
                var g = (c shr 8 and 0xff).toFloat()
                var b = (c and 0xff).toFloat()

                // 1. Skin Tone (Whitening)
                if (skinTone > 0.05f) {
                    val brightR = powerTable[r.toInt()]
                    val brightG = powerTable[g.toInt()]
                    val brightB = powerTable[b.toInt()]

                    val mixedR = r * (1.0f - skinTone) + brightR * skinTone
                    val mixedG = g * (1.0f - skinTone) + brightG * skinTone
                    val mixedB = b * (1.0f - skinTone) + brightB * skinTone

                    r = mixedR * (1.0f - skinTone * 0.15f) + (skinToneTargetR * mixedR / 255f) * (skinTone * 0.15f)
                    g = mixedG * (1.0f - skinTone * 0.15f) + (skinToneTargetG * mixedG / 255f) * (skinTone * 0.15f)
                    b = mixedB * (1.0f - skinTone * 0.15f) + (skinToneTargetB * mixedB / 255f) * (skinTone * 0.15f)
                }

                // 2. Skin Smoothing
                if (smoothing > 0.05f && landmarks != null && x in faceLeft..faceRight && y in faceTop..faceBottom) {
                    var sumR = r
                    var sumG = g
                    var sumB = b
                    var totalWeight = 1.0f
                    val step = 1 + (smoothing * 2).toInt() // 1 to 3

                    for (dy in -step..step) {
                        for (dx in -step..step) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in faceLeft..faceRight && ny in faceTop..faceBottom) {
                                val nc = pixels[ny * width + nx]
                                val nr = (nc shr 16 and 0xff).toFloat()
                                val ng = (nc shr 8 and 0xff).toFloat()
                                val nb = (nc and 0xff).toFloat()

                                val dist = Math.abs(r - nr) + Math.abs(g - ng) + Math.abs(b - nb)
                                val weight = Math.max(0.0f, 1.0f - (dist / 255f) * 3.0f) * smoothing
                                if (weight > 0f) {
                                    sumR += nr * weight
                                    sumG += ng * weight
                                    sumB += nb * weight
                                    totalWeight += weight
                                }
                            }
                        }
                    }
                    r = sumR / totalWeight
                    g = sumG / totalWeight
                    b = sumB / totalWeight
                }

                val ir = r.toInt().coerceIn(0, 255)
                val ig = g.toInt().coerceIn(0, 255)
                val ib = b.toInt().coerceIn(0, 255)

                outPixels[idx] = (a shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}
