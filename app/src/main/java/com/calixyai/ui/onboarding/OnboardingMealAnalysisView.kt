// OnboardingMealAnalysisView.kt - Tam Yenilənmiş Versiya
package com.calixyai.ui.onboarding

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.*

/**
 * Three-scene animated illustration for the third onboarding page.
 * Uses REAL meal images from drawable resources.
 */
class OnboardingMealAnalysisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Public callbacks ──────────────────────────────────────────────────────
    var onSceneChanged: ((scene: Int) -> Unit)? = null
    var onAnimationComplete: (() -> Unit)? = null

    // ── Scene state ───────────────────────────────────────────────────────────
    private var scene = 1          // 1 = camera, 2 = analysis, 3 = chat
    private var tick = 0f
    private var autoAdvanceTimer = 0f
    private var isAnimating = false

    // ── Colors ────────────────────────────────────────────────────────────────
    private val colorBg = Color.parseColor("#FAF7F2")
    private val colorGreen = Color.parseColor("#2D6A4F")
    private val colorGreenLight = Color.parseColor("#40C97A")
    private val colorFrameBg = Color.parseColor("#1A1A1A")
    private val colorTextPri = Color.parseColor("#1A2E1F")

    // ── Real Meal Images ──────────────────────────────────────────────────────
    private data class MealInfo(
        val name: String,
        val drawableResId: Int,
        val tags: Array<String>,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int,
        val score: Int,
        val introText: String
    )

    private val meals = listOf(
        MealInfo(
            name = "Buddha Bowl",
            drawableResId = getDrawableResourceId("meal_buddha_bowl"),
            tags = arrayOf("Quinoa", "Chickpeas", "Avocado", "Kale"),
            calories = 521,
            protein = 22,
            carbs = 68,
            fats = 18,
            score = 92,
            introText = "Great choice! This Buddha bowl is perfect for post-workout recovery."
        ),
        MealInfo(
            name = "Mediterranean Salad",
            drawableResId = getDrawableResourceId("meal_mediterranean_salad"),
            tags = arrayOf("Feta", "Olives", "Cucumber", "Tomato"),
            calories = 384,
            protein = 12,
            carbs = 28,
            fats = 24,
            score = 88,
            introText = "Excellent choice! This Mediterranean salad is nutrient-dense."
        ),
        MealInfo(
            name = "Protein Smoothie",
            drawableResId = getDrawableResourceId("meal_protein_smoothie"),
            tags = arrayOf("Whey", "Banana", "Berries", "Oat Milk"),
            calories = 425,
            protein = 32,
            carbs = 48,
            fats = 14,
            score = 95,
            introText = "Perfect post-workout smoothie! Here's the breakdown:",
        )
    )

    private var currentMealIndex = 0
    private var currentMeal = meals[0]
    private var mealBitmap: Bitmap? = null

    private fun getDrawableResourceId(name: String): Int {
        return resources.getIdentifier(name, "drawable", context.packageName)
    }

    // ── Paints ────────────────────────────────────────────────────────────────
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val textStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }

    // ── Animation state ───────────────────────────────────────────────────────
    private var scanY = 0f
    private var focusPulse = 0f
    private var sceneAlpha = 1f
    private var chatReveal = 0f
    private var analysisProgress = 0f
    private var macroRevealIndex = 0

    private data class Particle(var x: Float, var y: Float, var vy: Float, var alpha: Float, var phase: Float, var r: Float)
    private val particles = mutableListOf<Particle>()
    private var particlesSeeded = false

    // Animators
    private lateinit var scanAnimator: ValueAnimator
    private lateinit var focusAnimator: ValueAnimator
    private lateinit var crossFadeAnimator: ValueAnimator
    private lateinit var analysisProgressAnimator: ValueAnimator
    private lateinit var chatAnimator: ValueAnimator

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        isClickable = false
        loadMealImages()
    }

    private fun loadMealImages() {
        try {
            if (currentMeal.drawableResId != 0) {
                mealBitmap = ContextCompat.getDrawable(context, currentMeal.drawableResId)?.let { drawable ->
                    Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMealImage(index: Int) {
        currentMealIndex = index
        currentMeal = meals[index]

        val resId = currentMeal.drawableResId
        if (resId != 0) {
            mealBitmap = ContextCompat.getDrawable(context, resId)?.let { drawable ->
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }
        }
        invalidate()
    }

    // ── Animation Loop ────────────────────────────────────────────────────────
    private val frameRunnable = object : Runnable {
        override fun run() {
            tick += 1f

            when (scene) {
                1 -> {
                    tickCamera()
                    updateAutoAdvance()
                }
                2 -> {
                    tickAnalysis()
                    updateAnalysisProgress()
                }
                3 -> { /* driven by chatAnimator */ }
            }

            invalidate()
            postDelayed(this, 16L)
        }
    }

    private fun tickCamera() {
        focusPulse = (sin(tick * 0.06f) + 1f) / 2f
    }

    private fun updateAutoAdvance() {
        if (!isAnimating && scene == 1) {
            autoAdvanceTimer += 0.016f
            if (autoAdvanceTimer >= 2.5f) {
                advanceScene()
            }
        }
    }

    private fun updateAnalysisProgress() {
        if (!isAnimating && scene == 2 && analysisProgress >= 0.98f) {
            isAnimating = true
            postDelayed({ advanceScene() }, 600)
        }
    }

    private fun tickAnalysis() {
        if (!particlesSeeded) seedParticles()
        particles.forEach { p ->
            p.y -= p.vy
            p.phase += 0.05f
            p.alpha = (0.8f * sin(p.phase * PI.toFloat() / 3f)).coerceIn(0f, 0.9f)
            if (p.y < 0f) {
                val frameH = frameRect().height()
                p.y = frameH * 0.9f
                p.x = frameRect().left + (Math.random() * frameRect().width()).toFloat()
                p.phase = 0f
            }
        }

        when {
            analysisProgress > 0.7f && currentMealIndex < 2 && macroRevealIndex >= 4 -> updateMealImage(2)
            analysisProgress > 0.35f && currentMealIndex < 1 && macroRevealIndex >= 2 -> updateMealImage(1)
        }

        val newRevealIndex = (analysisProgress * 4).toInt().coerceIn(0, 4)
        if (newRevealIndex > macroRevealIndex) macroRevealIndex = newRevealIndex
    }

    private fun seedParticles() {
        particlesSeeded = true
        val fr = frameRect()
        repeat(10) {
            particles.add(Particle(
                x = fr.left + (Math.random() * fr.width()).toFloat(),
                y = fr.top + (Math.random() * fr.height()).toFloat(),
                vy = (1.0f + Math.random().toFloat() * 2f),
                alpha = 0f,
                phase = (Math.random() * 6f).toFloat(),
                r = (1.5f + Math.random().toFloat() * 2f)
            ))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initAnimators()
        startAnalysisProgress()
        post(frameRunnable)
        autoAdvanceTimer = 0f
        isAnimating = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
        if (::scanAnimator.isInitialized) scanAnimator.cancel()
        if (::focusAnimator.isInitialized) focusAnimator.cancel()
        if (::chatAnimator.isInitialized) chatAnimator.cancel()
        if (::crossFadeAnimator.isInitialized) crossFadeAnimator.cancel()
        if (::analysisProgressAnimator.isInitialized) analysisProgressAnimator.cancel()
    }

    private fun initAnimators() {
        scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { scanY = it.animatedValue as Float }
            start()
        }

        focusAnimator = ValueAnimator.ofFloat(0.3f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { focusPulse = it.animatedValue as Float }
            start()
        }
    }

    private fun startAnalysisProgress() {
        analysisProgressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4500L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                analysisProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun advanceScene() {
        if (scene >= 3) {
            onAnimationComplete?.invoke()
            return
        }

        isAnimating = true
        val nextScene = scene + 1

        crossFadeOut {
            scene = nextScene
            onSceneChanged?.invoke(scene)

            when (scene) {
                2 -> {
                    particlesSeeded = false
                    particles.clear()
                    macroRevealIndex = 0
                    currentMealIndex = 0
                    updateMealImage(0)
                    startAnalysisProgress()
                }
                3 -> startChatReveal()
            }

            crossFadeIn()
            isAnimating = false
        }
    }

    private fun crossFadeOut(onEnd: () -> Unit) {
        if (::crossFadeAnimator.isInitialized) crossFadeAnimator.cancel()
        crossFadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { sceneAlpha = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { onEnd() }
            })
            start()
        }
    }

    private fun crossFadeIn() {
        if (::crossFadeAnimator.isInitialized) crossFadeAnimator.cancel()
        crossFadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            addUpdateListener { sceneAlpha = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun startChatReveal() {
        chatReveal = 0f
        if (::chatAnimator.isInitialized) chatAnimator.cancel()
        chatAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500L
            interpolator = DecelerateInterpolator(0.8f)
            addUpdateListener {
                chatReveal = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onAnimationComplete?.invoke()
                }
            })
            start()
        }
    }

    // ── Layout helpers ────────────────────────────────────────────────────────
    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun frameRect(): RectF {
        val pad = dp(16f)
        return RectF(pad, dp(8f), width - pad, height * 0.64f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorBg)
        canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (sceneAlpha * 255).toInt())
        when (scene) {
            1 -> drawCameraScene(canvas)
            2 -> drawAnalysisScene(canvas)
            3 -> drawChatScene(canvas)
        }
        canvas.restore()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENE 1 — CAMERA
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawCameraScene(canvas: Canvas) {
        val fr = frameRect()
        val cornerR = dp(18f)

        fillPaint.color = colorFrameBg
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)

        mealBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = RectF(fr.left + dp(8f), fr.top + dp(8f), fr.right - dp(8f), fr.bottom - dp(64f))
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        }

        strokePaint.color = Color.argb(30, 255, 255, 255)
        strokePaint.strokeWidth = dp(0.5f)
        val gridStep = dp(56f)
        var gx = fr.left + gridStep
        while (gx < fr.right) { canvas.drawLine(gx, fr.top, gx, fr.bottom, strokePaint); gx += gridStep }
        var gy = fr.top + gridStep
        while (gy < fr.bottom) { canvas.drawLine(fr.left, gy, fr.right, gy, strokePaint); gy += gridStep }

        val cx = fr.centerX()
        val cy = fr.centerY() - dp(10f)
        val focusSize = dp(80f)
        val fx1 = cx - focusSize / 2f
        val fy1 = cy - focusSize / 2f
        val fx2 = cx + focusSize / 2f
        val fy2 = cy + focusSize / 2f
        val cornerLen = dp(15f)

        strokePaint.color = Color.argb((focusPulse * 230).toInt(), 64, 201, 122)
        strokePaint.strokeWidth = dp(2f)
        drawFocusCorners(canvas, fx1, fy1, fx2, fy2, cornerLen, Color.argb((255 * focusPulse).toInt().coerceIn(0, 255), 64, 201, 122))

        val barTop = fr.bottom - dp(56f)
        fillPaint.color = Color.argb(180, 0, 0, 0)
        val barPath = Path().apply {
            addRoundRect(RectF(fr.left, barTop, fr.right, fr.bottom), floatArrayOf(0f, 0f, 0f, 0f, cornerR, cornerR, cornerR, cornerR), Path.Direction.CW)
        }
        canvas.drawPath(barPath, fillPaint)

        textPaint.color = Color.argb(200, 255, 255, 255)
        textPaint.textSize = dp(11f)
        val dotAlpha = (sin(tick * 0.1f) + 1f) / 2f
        fillPaint.color = Color.argb((dotAlpha * 255).toInt(), 64, 201, 122)
        canvas.drawCircle(fr.centerX() - dp(50f), barTop + dp(28f), dp(5f), fillPaint)
        canvas.drawText("SCANNING MEAL...", fr.centerX() - dp(28f), barTop + dp(32f), textPaint)
    }

    private fun drawFocusCorners(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, len: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = dp(2f)
        strokePaint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(x1, y1 + len, x1, y1, strokePaint)
        canvas.drawLine(x1, y1, x1 + len, y1, strokePaint)
        canvas.drawLine(x2 - len, y1, x2, y1, strokePaint)
        canvas.drawLine(x2, y1, x2, y1 + len, strokePaint)
        canvas.drawLine(x1, y2 - len, x1, y2, strokePaint)
        canvas.drawLine(x1, y2, x1 + len, y2, strokePaint)
        canvas.drawLine(x2 - len, y2, x2, y2, strokePaint)
        canvas.drawLine(x2, y2, x2, y2 - len, strokePaint)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENE 2 — AI ANALYSIS
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawAnalysisScene(canvas: Canvas) {
        val fr = frameRect()
        val cornerR = dp(18f)

        fillPaint.color = Color.parseColor("#EDF6F0")
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)

        mealBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = RectF(fr.left + dp(8f), fr.top + dp(8f), fr.right - dp(8f), fr.bottom - dp(64f))
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        }

        canvas.save()
        canvas.clipRect(fr.left, fr.top, fr.right, fr.bottom)

        val scanActualY = fr.top + (fr.height() * scanY)
        val scanGrad = LinearGradient(fr.left, scanActualY, fr.right, scanActualY,
            intArrayOf(Color.TRANSPARENT, Color.argb(180, 64, 201, 122), Color.argb(220, 64, 201, 122), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
        fillPaint.shader = scanGrad
        canvas.drawRect(fr.left, scanActualY - dp(2f), fr.right, scanActualY + dp(2f), fillPaint)
        fillPaint.shader = null

        fillPaint.color = colorGreenLight
        particles.forEach { p ->
            fillPaint.alpha = (p.alpha * 200 * analysisProgress).toInt().coerceIn(0, 200)
            canvas.drawCircle(p.x, p.y, p.r, fillPaint)
        }
        fillPaint.alpha = 255
        canvas.restore()

        val statusTop = fr.bottom + dp(10f)
        val statusRect = RectF(dp(16f), statusTop, width - dp(16f), statusTop + dp(42f))
        fillPaint.color = Color.argb(25, 45, 106, 79)
        canvas.drawRoundRect(statusRect, dp(12f), dp(12f), fillPaint)

        val dotAlpha = (sin(tick * 0.1f) + 1f) / 2f
        fillPaint.color = Color.argb((dotAlpha * 255).toInt(), 45, 106, 79)
        canvas.drawCircle(statusRect.left + dp(18f), statusRect.centerY(), dp(5f), fillPaint)

        textStartPaint.color = colorGreen
        textStartPaint.textSize = dp(12f)
        val statusText = when {
            analysisProgress < 0.3f -> "Analyzing meal composition..."
            analysisProgress < 0.6f -> "Identifying ingredients..."
            analysisProgress < 0.9f -> "Calculating nutritional value..."
            else -> "Analysis complete!"
        }
        canvas.drawText(statusText, statusRect.left + dp(32f), statusRect.centerY() + dp(4f), textStartPaint)

        val progressBarRect = RectF(statusRect.left + dp(16f), statusRect.bottom - dp(12f), statusRect.right - dp(16f), statusRect.bottom - dp(6f))
        fillPaint.color = Color.argb(40, 45, 106, 79)
        canvas.drawRoundRect(progressBarRect, dp(3f), dp(3f), fillPaint)
        val progressWidth = (progressBarRect.width() * analysisProgress).coerceAtLeast(dp(2f))
        fillPaint.color = colorGreenLight
        canvas.drawRoundRect(RectF(progressBarRect.left, progressBarRect.top, progressBarRect.left + progressWidth, progressBarRect.bottom), dp(3f), dp(3f), fillPaint)

        val chipTop = statusRect.bottom + dp(10f)
        val chipW = (width - dp(32f) - dp(24f)) / 4f
        val chipVals = arrayOf(
            "${currentMeal.calories}",
            "${currentMeal.protein}g",
            "${currentMeal.carbs}g",
            "${currentMeal.fats}g"
        )
        val chipKeys = arrayOf("kcal", "protein", "carbs", "fats")

        chipVals.forEachIndexed { i, v ->
            val chipAlpha = if (i < macroRevealIndex) 1f else 0f
            if (chipAlpha > 0f) {
                val cx2 = dp(16f) + i * (chipW + dp(8f))
                val chipRect = RectF(cx2, chipTop, cx2 + chipW, chipTop + dp(50f))

                fillPaint.color = Color.argb((chipAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
                canvas.drawRoundRect(chipRect, dp(12f), dp(12f), fillPaint)

                textPaint.color = Color.argb((chipAlpha * 255).toInt(), 26, 46, 31)
                textPaint.textSize = dp(15f)
                canvas.drawText(v, chipRect.centerX(), chipRect.centerY() - dp(2f), textPaint)

                textPaint.color = Color.argb((chipAlpha * 255).toInt(), 122, 154, 126)
                textPaint.textSize = dp(9f)
                canvas.drawText(chipKeys[i], chipRect.centerX(), chipRect.centerY() + dp(13f), textPaint)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENE 3 — CHAT (Böyüdülmüş kart, textlər üst-üstə düşmür)
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawChatScene(canvas: Canvas) {
        val w = width.toFloat()
        var cursorY = dp(8f)

        // Header
        val headerH = dp(58f)
        fillPaint.color = colorBg
        canvas.drawRect(0f, 0f, w, headerH, fillPaint)

        val avR = dp(19f)
        val avCx = dp(20f) + avR
        val avCy = headerH / 2f
        fillPaint.color = colorGreen
        canvas.drawCircle(avCx, avCy, avR, fillPaint)
        textPaint.color = Color.WHITE
        textPaint.textSize = dp(12f)
        canvas.drawText("C", avCx, avCy + dp(4.5f), textPaint)

        textStartPaint.color = colorTextPri
        textStartPaint.textSize = dp(13f)
        canvas.drawText("Calixy Coach", avCx + avR + dp(10f), avCy - dp(2f), textStartPaint)
        textStartPaint.color = colorGreenLight
        textStartPaint.textSize = dp(10f)
        canvas.drawText("● Online now", avCx + avR + dp(10f), avCy + dp(12f), textStartPaint)

        cursorY = headerH + dp(14f)

        // Meal photo bubble
        val photoAlpha = (chatReveal / 0.2f).coerceIn(0f, 1f)
        val photoSize = dp(100f)
        val photoRight = w - dp(20f)
        val photoLeft = photoRight - photoSize
        val photoRect = RectF(photoLeft, cursorY, photoRight, cursorY + photoSize)

        fillPaint.color = Color.argb((photoAlpha * 255).toInt().coerceIn(0, 255), 220, 220, 220)
        canvas.drawRoundRect(photoRect, dp(14f), dp(14f), fillPaint)

        if (photoAlpha > 0.05f && mealBitmap != null) {
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(photoRect, dp(14f), dp(14f), Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(mealBitmap!!, Rect(0, 0, mealBitmap!!.width, mealBitmap!!.height), photoRect, null)
            canvas.restore()
        }

        cursorY += photoSize + dp(12f)

        // User bubble
        val userAlpha = ((chatReveal - 0.15f) / 0.2f).coerceIn(0f, 1f)
        val userText = "Is this good for my workout?"
        textPaint.textSize = dp(13f)
        val userTextW = textPaint.measureText(userText)
        val ubW = userTextW + dp(32f)
        val ubH = dp(44f)
        val ubRight = w - dp(20f)
        val ubLeft = ubRight - ubW
        val ubRect = RectF(ubLeft, cursorY, ubRight, cursorY + ubH)

        fillPaint.color = Color.argb((userAlpha * 255).toInt().coerceIn(0, 255), 45, 106, 79)
        canvas.drawRoundRect(ubRect, dp(18f), dp(18f), fillPaint)
        canvas.drawRect(ubRight - dp(14f), cursorY + ubH - dp(14f), ubRight, cursorY + ubH, fillPaint)
        textPaint.color = Color.argb((userAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawText(userText, ubRect.centerX(), ubRect.centerY() + dp(5f), textPaint)

        cursorY += ubH + dp(16f)

        // AI response
        val aiAlpha = ((chatReveal - 0.3f) / 0.25f).coerceIn(0f, 1f)
        if (aiAlpha > 0f) {
            val smR = dp(14f)
            val smCx = dp(20f) + smR
            val smCy = cursorY + smR
            fillPaint.color = Color.argb((aiAlpha * 255).toInt().coerceIn(0, 255), 45, 106, 79)
            canvas.drawCircle(smCx, smCy, smR, fillPaint)
            textPaint.color = Color.argb((aiAlpha * 255).toInt(), 255, 255, 255)
            textPaint.textSize = dp(9f)
            canvas.drawText("C", smCx, smCy + dp(3.5f), textPaint)

            val bubbleLeft = smCx + smR + dp(10f)
            val bubbleRight = w - dp(16f)
            val bubbleW = bubbleRight - bubbleLeft

            // AI intro text
            val introLines = wrapText(currentMeal.introText, bubbleW, dp(12.5f))
            var introHeight = 0f
            if (aiAlpha > 0f) {
                textStartPaint.textSize = dp(12.5f)
                textStartPaint.color = Color.argb((aiAlpha * 255).toInt(), 44, 62, 47)
                introLines.forEachIndexed { i, line ->
                    canvas.drawText(line, bubbleLeft, cursorY + dp(20f) + (i * dp(20f)), textStartPaint)
                }
                introHeight = dp(20f) + (introLines.size * dp(20f))
            }

            // ═══════════════════════════════════════════════════════════════════
            // NUTRITION BREAKDOWN CARD - BÖYÜDÜLMÜŞ VERSİYA
            // ═══════════════════════════════════════════════════════════════════
            val cardTop = cursorY + introHeight + dp(8f)
            val cardHeight = dp(135f)  // Böyüdüldü
            val cardRect = RectF(bubbleLeft, cardTop, bubbleRight, cardTop + cardHeight)

            fillPaint.color = Color.argb((aiAlpha * 255).toInt().coerceIn(0, 255), 244, 250, 246)
            canvas.drawRoundRect(cardRect, dp(14f), dp(14f), fillPaint)

            strokePaint.color = Color.argb((aiAlpha * 60).toInt(), 45, 106, 79)
            strokePaint.strokeWidth = dp(0.8f)
            strokePaint.style = Paint.Style.STROKE
            canvas.drawRoundRect(cardRect, dp(14f), dp(14f), strokePaint)
            strokePaint.style = Paint.Style.FILL

            // Header
            textStartPaint.textSize = dp(10f)
            textStartPaint.typeface = Typeface.DEFAULT_BOLD
            textStartPaint.color = Color.argb((aiAlpha * 255).toInt(), 45, 106, 79)
            canvas.drawText("NUTRITION BREAKDOWN", cardRect.left + dp(12f), cardRect.top + dp(20f), textStartPaint)

            // Score badge
            val badgeText = "Score ${currentMeal.score}/100"
            textPaint.textSize = dp(10f)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val badgeW = textPaint.measureText(badgeText) + dp(18f)
            val badgeH = dp(24f)
            val badgeRect = RectF(cardRect.right - badgeW - dp(10f), cardRect.top + dp(10f), cardRect.right - dp(10f), cardRect.top + dp(10f) + badgeH)
            fillPaint.color = Color.argb((aiAlpha * 35).toInt(), 45, 106, 79)
            canvas.drawRoundRect(badgeRect, dp(10f), dp(10f), fillPaint)
            textPaint.color = Color.argb((aiAlpha * 255).toInt(), 45, 106, 79)
            canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + dp(4f), textPaint)

            // Macros grid
            val macroVals = arrayOf(
                "${currentMeal.calories}",
                "${currentMeal.protein}g",
                "${currentMeal.carbs}g",
                "${currentMeal.fats}g"
            )
            val macroKeys = arrayOf("calories", "protein", "carbs", "fats")
            val half = bubbleW / 2f
            val gridTop = cardRect.top + dp(42f)
            val gridSpacing = dp(38f)

            macroVals.forEachIndexed { i, v ->
                val col = i % 2
                val row = i / 2
                val mx = bubbleLeft + col * half + dp(12f)
                val my = gridTop + row * gridSpacing

                textStartPaint.textSize = dp(18f)
                textStartPaint.typeface = Typeface.DEFAULT_BOLD
                textStartPaint.color = Color.argb((aiAlpha * 255).toInt(), 26, 46, 31)
                canvas.drawText(v, mx, my, textStartPaint)

                textStartPaint.textSize = dp(10f)
                textStartPaint.typeface = Typeface.DEFAULT
                textStartPaint.color = Color.argb((aiAlpha * 255).toInt(), 122, 154, 126)
                canvas.drawText(macroKeys[i], mx, my + dp(16f), textStartPaint)
            }

            // Separator xətti
            val separatorY = cardRect.bottom - dp(42f)
            strokePaint.color = Color.argb((aiAlpha * 30).toInt(), 45, 106, 79)
            strokePaint.strokeWidth = dp(0.8f)
            canvas.drawLine(cardRect.left + dp(12f), separatorY, cardRect.right - dp(12f), separatorY, strokePaint)

            // Advice text
            textStartPaint.textSize = dp(11f)
            textStartPaint.typeface = Typeface.DEFAULT
            textStartPaint.color = Color.argb((aiAlpha * 255).toInt(), 62, 107, 79)


        }
    }

    // ── Text utilities ────────────────────────────────────────────────────────
    private fun wrapText(text: String, maxWidth: Float, textSize: Float): List<String> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
        }
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}