package com.tejaslabs.beauty.ai.camera.renderer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.tejaslabs.beauty.ai.camera.detector.FaceLandmarksData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraGLRenderer(
    private val onSurfaceCreatedCallback: (SurfaceTexture) -> Unit
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "CameraGLRenderer"

    // Shader sources
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        varying vec2 vQuadCoord;
        
        void main() {
            gl_Position = aPosition;
            vQuadCoord = aTexCoord.xy;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vQuadCoord;
        
        uniform samplerExternalOES sTexture;
        uniform mat4 uTexMatrix;
        
        // Beauty Adjustments
        uniform float uSmoothing;
        uniform float uBrightness;
        uniform float uSkinTone;
        
        // Eye Enlargement
        uniform vec2 uLeftEyeCenter;
        uniform vec2 uRightEyeCenter;
        uniform float uEyeEnlargement;
        uniform float uEyeRadius;
        
        // Face Slimming
        uniform vec2 uFaceCenter;
        uniform float uFaceSlimming;
        uniform float uFaceRadius;
        
        // Filters
        uniform int uFilterType;
        uniform float uFilterIntensity;
        
        // Dimensions
        uniform float uTexWidth;
        uniform float uTexHeight;
        uniform float uScaleX;
        uniform float uScaleY;
        
        // Bulge warp for eye enlargement
        vec2 bulgeWarp(vec2 uv, vec2 center, float radius, float intensity) {
            if (intensity < 0.01 || center.x < 0.01 || center.y < 0.01) {
                return uv;
            }
            float aspect = uTexWidth / uTexHeight;
            vec2 distVec = uv - center;
            distVec.y /= aspect; // aspect ratio correction
            float dist = length(distVec);
            
            if (dist < radius) {
                float percent = dist / radius;
                // Bulge: pull pixels inward so the texture is stretched outward
                float strength = 1.0 - (1.0 - percent) * (1.0 - percent) * intensity * 0.35;
                vec2 warpedDistVec = distVec * strength;
                warpedDistVec.y *= aspect; // restore aspect ratio
                return center + warpedDistVec;
            }
            return uv;
        }
        
        // Pinch warp for face slimming
        vec2 faceSlimWarp(vec2 uv, vec2 center, float radius, float intensity) {
            if (intensity < 0.01 || center.x < 0.01 || center.y < 0.01) {
                return uv;
            }
            float aspect = uTexWidth / uTexHeight;
            vec2 distVec = uv - center;
            distVec.y /= aspect;
            float dist = length(distVec);
            
            // Apply slimming below the nose/eyes (uv.y < center.y + 0.02 in OpenGL coordinates where 0 is bottom)
            if (dist < radius && uv.y < center.y + 0.02) {
                float percent = dist / radius;
                float strength = (1.0 - percent) * (1.0 - percent) * intensity * 0.15;
                vec2 warped = uv;
                float dx = uv.x - center.x;
                warped.x = center.x + dx * (1.0 + strength);
                return warped;
            }
            return uv;
        }
        
        // Bilateral filter approximation for skin smoothing
        vec4 applySmoothing(vec2 uv, float intensity) {
            if (intensity < 0.05) {
                return texture2D(sTexture, uv);
            }
            
            vec4 centerCol = texture2D(sTexture, uv);
            vec3 sum = centerCol.rgb;
            float totalWeight = 1.0;
            
            float stepX = (2.0 + intensity * 8.0) / uTexWidth;
            float stepY = (2.0 + intensity * 8.0) / uTexHeight;
            
            // 3x3 offsets
            vec2 offsets[8];
            offsets[0] = vec2(-stepX, -stepY);
            offsets[1] = vec2(0.0, -stepY);
            offsets[2] = vec2(stepX, -stepY);
            offsets[3] = vec2(-stepX, 0.0);
            offsets[4] = vec2(stepX, 0.0);
            offsets[5] = vec2(-stepX, stepY);
            offsets[6] = vec2(0.0, stepY);
            offsets[7] = vec2(stepX, stepY);
            
            for (int i = 0; i < 8; i++) {
                vec4 sampleCol = texture2D(sTexture, uv + offsets[i]);
                float colorDist = distance(centerCol.rgb, sampleCol.rgb);
                
                // Keep edges sharp, blur skin-like uniform areas
                float weight = max(0.0, 1.0 - colorDist * 3.0);
                weight *= intensity;
                
                sum += sampleCol.rgb * weight;
                totalWeight += weight;
            }
            
            return vec4(sum / totalWeight, centerCol.a);
        }
        
        void main() {
            vec2 scales = vec2(max(uScaleX, 0.01), max(uScaleY, 0.01));
            vec2 leftEyeCenterScreen = vec2(0.5) + (uLeftEyeCenter - vec2(0.5)) / scales;
            vec2 rightEyeCenterScreen = vec2(0.5) + (uRightEyeCenter - vec2(0.5)) / scales;
            vec2 faceCenterScreen = vec2(0.5) + (uFaceCenter - vec2(0.5)) / scales;
            
            // 1. Warp the coordinates in screen space
            vec2 warpedUV = vQuadCoord;
            if (uEyeEnlargement > 0.0) {
                warpedUV = bulgeWarp(warpedUV, leftEyeCenterScreen, uEyeRadius, uEyeEnlargement);
                warpedUV = bulgeWarp(warpedUV, rightEyeCenterScreen, uEyeRadius, uEyeEnlargement);
            }
            if (uFaceSlimming > 0.0) {
                warpedUV = faceSlimWarp(warpedUV, faceCenterScreen, uFaceRadius, uFaceSlimming);
            }
            
            // 2. Map warped screen coordinates back to camera coordinates
            vec2 finalCamUV = vec2(0.5) + (warpedUV - vec2(0.5)) * vec2(uScaleX, uScaleY);
            
            // 3. Map camera coordinates to external camera texture coordinates
            vec2 texCoord = (uTexMatrix * vec4(finalCamUV, 0.0, 1.0)).xy;
            
            // 3. Apply bilateral skin smoothing
            vec4 color = applySmoothing(texCoord, uSmoothing);
            
            // 4. Apply Brightness
            color.rgb += uBrightness;
            
            // 5. Apply Skin Tone (Whitening)
            if (uSkinTone > 0.0) {
                vec3 skinToneTarget = vec3(1.0, 0.94, 0.94);
                vec3 brightened = pow(max(color.rgb, vec3(0.0)), vec3(1.0 - uSkinTone * 0.25));
                color.rgb = mix(color.rgb, brightened, uSkinTone);
                color.rgb = mix(color.rgb, skinToneTarget * color.rgb, uSkinTone * 0.15);
            }
            
            // 6. Apply Filter Color Grading
            vec3 filtered = color.rgb;
            if (uFilterType == 0) {
                // Natural
                filtered = mix(color.rgb, color.rgb * vec3(1.06, 1.02, 1.04), 0.5);
            } else if (uFilterType == 1) {
                // Warm
                filtered = color.rgb * vec3(1.12, 1.06, 0.92);
            } else if (uFilterType == 2) {
                // Cool
                filtered = color.rgb * vec3(0.92, 1.02, 1.15);
            } else if (uFilterType == 3) {
                // Vintage
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                vec3 sepia = vec3(gray) * vec3(1.0, 0.88, 0.73);
                filtered = mix(color.rgb, sepia, 0.7);
            } else if (uFilterType == 4) {
                // Pink
                filtered = color.rgb * vec3(1.16, 0.94, 1.06);
            } else if (uFilterType == 5) {
                // Film
                vec3 film = pow(max(color.rgb, vec3(0.0)), vec3(1.15));
                film = mix(film, vec3(dot(film, vec3(0.299, 0.587, 0.114))), 0.15);
                film += vec3(0.04, 0.02, 0.05);
                filtered = film;
            } else if (uFilterType == 6) {
                // Bright
                filtered = color.rgb * 1.15;
            } else if (uFilterType == 7) {
                // Soft
                vec3 soft = mix(color.rgb, vec3(0.5), -0.12);
                filtered = soft * vec3(1.04, 1.02, 1.04);
            } else if (uFilterType == 8) {
                // Black & White
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                filtered = vec3(gray);
            } else if (uFilterType == 9) {
                // Sunset
                filtered = color.rgb * vec3(1.18, 0.96, 0.82);
            } else if (uFilterType == 10) {
                // Glow
                vec3 glow = color.rgb + max(color.rgb - vec3(0.6), vec3(0.0)) * 0.4;
                filtered = glow;
            } else if (uFilterType == 11) {
                // Fresh
                filtered = color.rgb * vec3(0.96, 1.12, 1.04);
            } else if (uFilterType == 12) {
                // Dream
                vec3 dream = mix(color.rgb, vec3(1.0, 0.9, 0.96), 0.18);
                filtered = dream;
            } else if (uFilterType == 13) {
                // Matte
                vec3 matte = color.rgb * 0.92 + vec3(0.04, 0.04, 0.05);
                filtered = matte;
            } else if (uFilterType == 14) {
                // Classic
                vec3 classic = pow(max(color.rgb, vec3(0.0)), vec3(1.2));
                filtered = classic * 1.05;
            }
            
            color.rgb = mix(color.rgb, filtered, uFilterIntensity);
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """.trimIndent()

    // Buffers for quad geometry
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private val vertices = floatArrayOf(
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f,
         1.0f,  1.0f, 0.0f
    )

    private val texCoords = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )

    private var programId = 0
    private var textureId = 0
    var surfaceTexture: SurfaceTexture? = null

    // Transform matrices
    private val texMatrix = FloatArray(16)

    // Volatile parameters updated from the UI thread
    @Volatile var smoothing = 0.40f
    @Volatile var brightness = 0.0f
    @Volatile var skinTone = 0.20f
    @Volatile var eyeEnlargement = 0.0f
    @Volatile var faceSlimming = 0.0f
    @Volatile var filterType = 0
    @Volatile var filterIntensity = 0.60f
    @Volatile var landmarks: FaceLandmarksData? = null
    @Volatile var isFrontCamera = true
    @Volatile var cameraWidth = 1080f
    @Volatile var cameraHeight = 1920f

    private var width = 1080f
    private var height = 1920f

    init {
        // Initialize buffers
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        programId = GLShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode)
        if (programId == 0) {
            Log.e(TAG, "Shader program creation failed")
            return
        }

        textureId = GLShaderUtils.createOESTexture()
        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener(this@CameraGLRenderer)
        }

        surfaceTexture?.let {
            onSurfaceCreatedCallback(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val st = surfaceTexture ?: return
        synchronized(this) {
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)
        }

        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Bind OES texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        val uTextureLoc = GLES20.glGetUniformLocation(programId, "sTexture")
        GLES20.glUniform1i(uTextureLoc, 0)

        // Pass transform matrix
        val uTexMatrixLoc = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

        // Pass beauty params
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uSmoothing"), smoothing)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uBrightness"), brightness)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uSkinTone"), skinTone)

        // Pass eye landmarks and intensities
        val leftEyeCenterLoc = GLES20.glGetUniformLocation(programId, "uLeftEyeCenter")
        val rightEyeCenterLoc = GLES20.glGetUniformLocation(programId, "uRightEyeCenter")
        val eyeEnlargeLoc = GLES20.glGetUniformLocation(programId, "uEyeEnlargement")
        val eyeRadiusLoc = GLES20.glGetUniformLocation(programId, "uEyeRadius")

        val currentLandmarks = landmarks
        if (currentLandmarks != null) {
            // Front camera is horizontally mirrored on screen.
            // Landmarker is run on the unmirrored bitmap.
            // Flip the X coordinate for the front camera so it lines up with the mirrored viewport coordinates.
            var lx = currentLandmarks.leftEyeX
            var rx = currentLandmarks.rightEyeX
            var nx = currentLandmarks.noseX

            if (isFrontCamera) {
                lx = 1.0f - lx
                rx = 1.0f - rx
                nx = 1.0f - nx
            }

            GLES20.glUniform2f(leftEyeCenterLoc, lx, 1.0f - currentLandmarks.leftEyeY)
            GLES20.glUniform2f(rightEyeCenterLoc, rx, 1.0f - currentLandmarks.rightEyeY)
            GLES20.glUniform1f(eyeEnlargeLoc, eyeEnlargement)
            GLES20.glUniform1f(eyeRadiusLoc, 0.06f) // Standard radial boundary

            // Pass face slimming params
            val faceCenterLoc = GLES20.glGetUniformLocation(programId, "uFaceCenter")
            val faceSlimLoc = GLES20.glGetUniformLocation(programId, "uFaceSlimming")
            val faceRadiusLoc = GLES20.glGetUniformLocation(programId, "uFaceRadius")

            GLES20.glUniform2f(faceCenterLoc, nx, 1.0f - currentLandmarks.noseY)
            GLES20.glUniform1f(faceSlimLoc, faceSlimming)
            GLES20.glUniform1f(faceRadiusLoc, 0.15f) // Slimming boundary
        } else {
            // Reset to prevent warping if no face is detected
            GLES20.glUniform2f(leftEyeCenterLoc, 0f, 0f)
            GLES20.glUniform2f(rightEyeCenterLoc, 0f, 0f)
            GLES20.glUniform1f(eyeEnlargeLoc, 0f)
            GLES20.glUniform1f(eyeRadiusLoc, 0f)

            GLES20.glUniform2f(GLES20.glGetUniformLocation(programId, "uFaceCenter"), 0f, 0f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFaceSlimming"), 0f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFaceRadius"), 0f)
        }

        // Pass filter details
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "uFilterType"), filterType)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFilterIntensity"), filterIntensity)

        // Pass viewport sizes
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uTexWidth"), width)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uTexHeight"), height)

        val cameraAspect = if (cameraWidth > cameraHeight) {
            cameraHeight / cameraWidth
        } else {
            cameraWidth / cameraHeight
        }
        val viewportAspect = if (height > 0) width / height else 9f / 16f

        var scaleX = 1.0f
        var scaleY = 1.0f

        if (viewportAspect > cameraAspect) {
            scaleY = cameraAspect / viewportAspect
        } else {
            scaleX = viewportAspect / cameraAspect
        }

        val uScaleXLoc = GLES20.glGetUniformLocation(programId, "uScaleX")
        val uScaleYLoc = GLES20.glGetUniformLocation(programId, "uScaleY")
        GLES20.glUniform1f(uScaleXLoc, scaleX)
        GLES20.glUniform1f(uScaleYLoc, scaleY)

        // Pass positions
        val aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        // Pass texture coordinates
        val aTexCoordLoc = GLES20.glGetAttribLocation(programId, "aTexCoord")
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        // Frame available on the CameraX texture, request a render pass from the GL view
        // Note: The actual view will call requestRender()
    }
}
