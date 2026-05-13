package com.calixyai.ui.onboarding

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.*

/**
 * Three-scene animated illustration for the third onboarding page.
 *
 * Scene 1 → CAMERA   : iPhone-style viewfinder with auto-focus bracket + meal bowl.
 * Scene 2 → ANALYSIS : Scan-line, floating particles, corner brackets, ingredient tags.
 * Scene 3 → CHAT     : Meal photo bubble, user question, AI nutrition response card.
 *
 * Tap the shutter (scene 1) or the forward arrow (scenes 2-3) to advance.
 * The host fragment should observe [onSceneChanged] to update its dot indicator.
 */
class OnboardingMealAnalysisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Public callback ───────────────────────────────────────────────────────
    var onSceneChanged: ((scene: Int) -> Unit)? = null

    // ── Scene state ───────────────────────────────────────────────────────────
    private var scene = 1          // 1 = camera, 2 = analysis, 3 = chat
    private var tick  = 0f

    // ── Colors ────────────────────────────────────────────────────────────────
    private val colorBg         = Color.parseColor("#FAF7F2")
    private val colorGreen      = Color.parseColor("#2D6A4F")
    private val colorGreenLight = Color.parseColor("#40C97A")
    private val colorGreenFade  = Color.parseColor("#52B788")
    private val colorFrameBg    = Color.parseColor("#1A1A1A")
    private val colorBowl       = Color.parseColor("#6B4E3D")
    private val colorBowlRim    = Color.parseColor("#8B6355")
    private val colorProtein    = Color.parseColor("#C8A882")
    private val colorGreens     = Color.parseColor("#4A7C59")
    private val colorGrain      = Color.parseColor("#D4B483")
    private val colorSurface    = Color.parseColor("#FFFFFF")
    private val colorCardBg     = Color.parseColor("#F4FAF6")
    private val colorTextPri    = Color.parseColor("#1A2E1F")
    private val colorTextSec    = Color.parseColor("#6B7B6E")
    private val colorTextMuted  = Color.parseColor("#9EAA9F")
    private val colorUserBubble = Color.parseColor("#2D6A4F")
    private val colorBotBubble  = Color.parseColor("#FFFFFF")
    private val colorStroke     = Color.parseColor("#E8F0EA")

    // ── Paints ────────────────────────────────────────────────────────────────
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val textStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }

    // ── Animation state ───────────────────────────────────────────────────────
    private var scanY        = 0f   // 0..1 scan line progress
    private var focusPulse   = 0f   // 0..1 for focus ring alpha
    private var orbitAngle   = 0f
    private var sceneAlpha   = 1f   // cross-fade alpha
    private var chatReveal   = 0f   // 0..1 chat message reveal progress

    // Tag fade-in times (scene 2)
    private val tagDelays  = floatArrayOf(60f, 110f, 160f, 210f)
    private val tagLabels  = arrayOf("Grilled chicken", "Brown rice", "Avocado", "Mixed greens")

    // Particles
    private data class Particle(
        var x: Float, var y: Float,
        var vy: Float, var alpha: Float, var phase: Float, var r: Float
    )
    private val particles = mutableListOf<Particle>()
    private var particlesSeeded = false

    // Chat reveal animator
    private lateinit var chatAnimator: ValueAnimator
    private lateinit var scanAnimator: ValueAnimator
    private lateinit var focusAnimator: ValueAnimator
    private lateinit var crossFadeAnimator: ValueAnimator

    // ── Frame loop ────────────────────────────────────────────────────────────
    private val frameRunnable = object : Runnable {
        override fun run() {
            tick += 1f
            orbitAngle += 0.006f
            when (scene) {
                1 -> tickCamera()
                2 -> tickAnalysis()
                3 -> { /* driven by chatAnimator */ }
            }
            invalidate()
            postDelayed(this, 16L)
        }
    }

    private fun tickCamera() {
        focusPulse = (sin(tick * 0.06f) + 1f) / 2f  // 0..1
    }

    private fun tickAnalysis() {
        if (!particlesSeeded) seedParticles()
        particles.forEach { p ->
            p.y   -= p.vy
            p.phase += 0.05f
            p.alpha = (0.8f * sin(p.phase * PI.toFloat() / 3f)).coerceIn(0f, 0.9f)
            if (p.y < 0f) {
                val frameH = frameRect().height()
                p.y = frameH * 0.9f
                p.x = frameRect().left + (Math.random() * frameRect().width()).toFloat()
                p.phase = 0f
            }
        }
    }

    private fun seedParticles() {
        particlesSeeded = true
        val fr = frameRect()
        repeat(8) {
            particles.add(Particle(
                x     = fr.left + (Math.random() * fr.width()).toFloat(),
                y     = fr.top  + (Math.random() * fr.height()).toFloat(),
                vy    = (1.5f + Math.random().toFloat() * 2f),
                alpha = 0f,
                phase = (Math.random() * 6f).toFloat(),
                r     = (2f + Math.random().toFloat() * 2.5f)
            ))
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initAnimators()
        post(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
        if (::scanAnimator.isInitialized)     scanAnimator.cancel()
        if (::focusAnimator.isInitialized)    focusAnimator.cancel()
        if (::chatAnimator.isInitialized)     chatAnimator.cancel()
        if (::crossFadeAnimator.isInitialized) crossFadeAnimator.cancel()
    }

    private fun initAnimators() {
        scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration        = 2500L
            repeatCount     = ValueAnimator.INFINITE
            repeatMode      = ValueAnimator.RESTART
            interpolator    = LinearInterpolator()
            addUpdateListener { scanY = it.animatedValue as Float }
            start()
        }
        focusAnimator = ValueAnimator.ofFloat(0.3f, 1f).apply {
            duration     = 1400L
            repeatCount  = ValueAnimator.INFINITE
            repeatMode   = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { focusPulse = it.animatedValue as Float }
        }
    }

    // ── Scene advance ─────────────────────────────────────────────────────────
    private fun advanceScene() {
        if (scene >= 3) return
        val nextScene = scene + 1
        crossFadeOut {
            scene = nextScene
            onSceneChanged?.invoke(scene)
            particlesSeeded = false
            particles.clear()
            crossFadeIn()
            if (scene == 3) startChatReveal()
        }
    }

    private fun crossFadeOut(onEnd: () -> Unit) {
        if (::crossFadeAnimator.isInitialized) crossFadeAnimator.cancel()
        crossFadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 250L
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
            duration     = 350L
            interpolator = DecelerateInterpolator()
            addUpdateListener { sceneAlpha = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun startChatReveal() {
        chatReveal = 0f
        if (::chatAnimator.isInitialized) chatAnimator.cancel()
        chatAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration     = 1800L
            interpolator = DecelerateInterpolator(0.8f)
            addUpdateListener { chatReveal = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    // ── Touch ─────────────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (scene) {
                1 -> {
                    val sb = shutterRect()
                    if (sb.contains(event.x, event.y)) { advanceScene(); return true }
                }
                2, 3 -> {
                    val ab = arrowRect()
                    if (ab.contains(event.x, event.y)) { advanceScene(); return true }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // ── Layout helpers ────────────────────────────────────────────────────────
    private fun dp(v: Float) = v * resources.displayMetrics.density

    /** The rounded camera/analysis frame rect */
    private fun frameRect(): RectF {
        val pad = dp(16f)
        return RectF(pad, dp(8f), width - pad, height * 0.64f)
    }

    private fun shutterRect(): RectF {
        val cx = width / 2f
        val cy = frameRect().bottom - dp(32f)
        val r  = dp(26f)
        return RectF(cx - r, cy - r, cx + r, cy + r)
    }

    private fun arrowRect(): RectF {
        val r = dp(20f)
        val cx = width - dp(28f)
        val cy = frameRect().bottom - dp(28f)
        return RectF(cx - r, cy - r, cx + r, cy + r)
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
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

        // Dark frame background
        fillPaint.color = colorFrameBg
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)

        // Grid overlay
        strokePaint.color = Color.argb(20, 255, 255, 255)
        strokePaint.strokeWidth = dp(0.5f)
        val gridStep = dp(56f)
        var gx = fr.left + gridStep
        while (gx < fr.right) { canvas.drawLine(gx, fr.top, gx, fr.bottom, strokePaint); gx += gridStep }
        var gy = fr.top + gridStep
        while (gy < fr.bottom) { canvas.drawLine(fr.left, gy, fr.right, gy, strokePaint); gy += gridStep }

        // Meal bowl
        val cx = fr.centerX(); val cy = fr.centerY() - dp(10f)
        drawMealBowl(canvas, cx, cy, dp(56f), 1f)

        // Auto-focus bracket
        val focusSize = dp(68f)
        val fx1 = cx - focusSize / 2f; val fy1 = cy - focusSize / 2f
        val fx2 = cx + focusSize / 2f; val fy2 = cy + focusSize / 2f
        val cornerLen = dp(12f)
        strokePaint.color = Color.argb((focusPulse * 230).toInt(), 255, 255, 255)
        strokePaint.strokeWidth = dp(1.4f)
        drawFocusCorners(canvas, fx1, fy1, fx2, fy2, cornerLen, Color.argb((255 * focusPulse).toInt().coerceIn(0,255), 64, 201, 122))

        // Dim overlay (vignette)
        val vignette = RadialGradient(cx, cy, fr.width() * 0.7f,
            intArrayOf(Color.TRANSPARENT, Color.argb(100, 0, 0, 0)),
            null, Shader.TileMode.CLAMP)
        fillPaint.shader = vignette
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)
        fillPaint.shader = null

        // Bottom controls bar
        val barTop = fr.bottom - dp(64f)
        fillPaint.color = Color.argb(130, 0, 0, 0)
        val barPath = Path().apply {
            addRoundRect(RectF(fr.left, barTop, fr.right, fr.bottom), floatArrayOf(0f,0f,0f,0f,cornerR,cornerR,cornerR,cornerR), Path.Direction.CW)
        }
        canvas.drawPath(barPath, fillPaint)

        // Labels
        textPaint.color = Color.argb(160, 255, 255, 255)
        textPaint.textSize = dp(10f)
        canvas.drawText("MEAL", fr.left + dp(28f), barTop + dp(28f), textPaint)
        canvas.drawText("IDENTIFY", fr.right - dp(28f), barTop + dp(28f), textPaint)

        // Shutter button
        val sb = shutterRect()
        fillPaint.color = Color.argb(220, 255, 255, 255)
        strokePaint.color = Color.argb(80, 255, 255, 255)
        strokePaint.strokeWidth = dp(3f)
        canvas.drawCircle(sb.centerX(), sb.centerY(), sb.width() / 2f, fillPaint)
        strokePaint.style = Paint.Style.STROKE
        canvas.drawCircle(sb.centerX(), sb.centerY(), sb.width() / 2f + dp(4f), strokePaint)
        strokePaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        canvas.drawCircle(sb.centerX(), sb.centerY(), sb.width() / 2f - dp(4f), fillPaint)
    }

    private fun drawFocusCorners(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, len: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = dp(2f)
        strokePaint.strokeCap = Paint.Cap.ROUND
        // Top-left
        canvas.drawLine(x1, y1 + len, x1, y1, strokePaint)
        canvas.drawLine(x1, y1, x1 + len, y1, strokePaint)
        // Top-right
        canvas.drawLine(x2 - len, y1, x2, y1, strokePaint)
        canvas.drawLine(x2, y1, x2, y1 + len, strokePaint)
        // Bottom-left
        canvas.drawLine(x1, y2 - len, x1, y2, strokePaint)
        canvas.drawLine(x1, y2, x1 + len, y2, strokePaint)
        // Bottom-right
        canvas.drawLine(x2 - len, y2, x2, y2, strokePaint)
        canvas.drawLine(x2, y2, x2, y2 - len, strokePaint)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENE 2 — AI ANALYSIS
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawAnalysisScene(canvas: Canvas) {
        val fr = frameRect()
        val cornerR = dp(18f)

        // Soft green frame bg
        fillPaint.color = Color.parseColor("#EDF6F0")
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)

        // Dim overlay
        fillPaint.color = Color.argb(30, 15, 40, 25)
        canvas.drawRoundRect(fr, cornerR, cornerR, fillPaint)

        // Meal bowl (desaturated, slightly smaller)
        drawMealBowl(canvas, fr.centerX(), fr.centerY() - dp(14f), dp(46f), 0.75f)

        // Scan line (clip to frame)
        canvas.save()
        canvas.clipRect(fr.left, fr.top, fr.right, fr.bottom)
        val scanActualY = fr.top + (fr.height() * scanY)
        val scanGrad = LinearGradient(fr.left, scanActualY, fr.right, scanActualY,
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 45, 106, 79), Color.argb(200, 64, 201, 122), Color.argb(160, 45, 106, 79), Color.TRANSPARENT),
            floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f), Shader.TileMode.CLAMP)
        fillPaint.shader = scanGrad
        canvas.drawRect(fr.left, scanActualY - dp(1f), fr.right, scanActualY + dp(1.5f), fillPaint)
        fillPaint.shader = null

        // Particles
        fillPaint.color = colorGreenLight
        particles.forEach { p ->
            fillPaint.alpha = (p.alpha * 200).toInt().coerceIn(0, 200)
            canvas.drawCircle(p.x, p.y, p.r, fillPaint)
        }
        fillPaint.alpha = 255
        canvas.restore()

        // Corner brackets (on top of clip)
        val bracketLen = dp(14f)
        val inset = dp(14f)
        drawFocusCorners(canvas,
            fr.left + inset, fr.top + inset,
            fr.right - inset, fr.bottom - dp(52f) - inset,
            bracketLen, colorGreenLight
        )

        // Ingredient tags (bottom of frame)
        var tagX = fr.left + dp(10f)
        val tagY  = fr.bottom - dp(44f)
        tagLabels.forEachIndexed { i, label ->
            val delay = tagDelays[i]
            val tagAlpha = ((tick - delay) / 30f).coerceIn(0f, 1f)
            if (tagAlpha > 0f) {
                textPaint.textSize  = dp(9.5f)
                textPaint.color     = Color.WHITE
                val tagW = textPaint.measureText(label) + dp(18f)
                val tagH = dp(22f)
                val tagRect = RectF(tagX, tagY, tagX + tagW, tagY + tagH)

                fillPaint.color = Color.argb((tagAlpha * 220).toInt(), 255, 255, 255)
                canvas.drawRoundRect(tagRect, tagH / 2f, tagH / 2f, fillPaint)

                strokePaint.color = Color.argb((tagAlpha * 60).toInt(), 45, 106, 79)
                strokePaint.strokeWidth = dp(0.5f)
                strokePaint.style = Paint.Style.STROKE
                canvas.drawRoundRect(tagRect, tagH / 2f, tagH / 2f, strokePaint)
                strokePaint.style = Paint.Style.FILL

                textPaint.color = Color.argb((tagAlpha * 255).toInt(), 26, 77, 46)
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(label, tagRect.centerX(), tagRect.centerY() + dp(3.5f), textPaint)
                textPaint.textAlign = Paint.Align.CENTER

                tagX += tagW + dp(6f)
                if (tagX > fr.right - dp(10f)) tagX = fr.left + dp(10f)
            }
        }

        // Forward arrow
        drawForwardArrow(canvas)

        // Status bar below frame
        val statusTop = fr.bottom + dp(10f)
        val statusRect = RectF(dp(16f), statusTop, width - dp(16f), statusTop + dp(42f))
        fillPaint.color = Color.argb(25, 45, 106, 79)
        canvas.drawRoundRect(statusRect, dp(12f), dp(12f), fillPaint)
        strokePaint.color = Color.argb(40, 45, 106, 79)
        strokePaint.strokeWidth = dp(0.5f)
        strokePaint.style = Paint.Style.STROKE
        canvas.drawRoundRect(statusRect, dp(12f), dp(12f), strokePaint)
        strokePaint.style = Paint.Style.FILL

        // Pulsing dot
        val dotAlpha = (sin(tick * 0.1f) + 1f) / 2f
        fillPaint.color = Color.argb((dotAlpha * 255).toInt(), 45, 106, 79)
        canvas.drawCircle(statusRect.left + dp(18f), statusRect.centerY(), dp(4f), fillPaint)

        textStartPaint.color = colorGreen
        textStartPaint.textSize = dp(12f)
        canvas.drawText("Analyzing nutritional profile…", statusRect.left + dp(30f), statusRect.centerY() + dp(4f), textStartPaint)

        // Macro chips
        val chipTop  = statusRect.bottom + dp(10f)
        val chipW    = (width - dp(32f) - dp(24f)) / 4f
        val chipVals = arrayOf("487", "38g", "42g", "14g")
        val chipKeys = arrayOf("kcal", "protein", "carbs", "fats")
        chipVals.forEachIndexed { i, v ->
            val chipAlpha = ((tick - 100f - i * 20f) / 30f).coerceIn(0f, 1f)
            val cx2 = dp(16f) + i * (chipW + dp(8f))
            val chipRect = RectF(cx2, chipTop, cx2 + chipW, chipTop + dp(50f))
            fillPaint.color = Color.argb((chipAlpha * 255).toInt().coerceIn(0,255), 255, 255, 255)
            canvas.drawRoundRect(chipRect, dp(12f), dp(12f), fillPaint)
            strokePaint.color = Color.argb((chipAlpha * 30).toInt(), 45, 106, 79)
            strokePaint.strokeWidth = dp(0.5f)
            strokePaint.style = Paint.Style.STROKE
            canvas.drawRoundRect(chipRect, dp(12f), dp(12f), strokePaint)
            strokePaint.style = Paint.Style.FILL
            textPaint.color = Color.argb((chipAlpha * 255).toInt(), 26, 46, 31)
            textPaint.textSize = dp(15f)
            canvas.drawText(v, chipRect.centerX(), chipRect.centerY() - dp(2f), textPaint)
            textPaint.color = Color.argb((chipAlpha * 255).toInt(), 122, 154, 126)
            textPaint.textSize = dp(9f)
            canvas.drawText(chipKeys[i], chipRect.centerX(), chipRect.centerY() + dp(13f), textPaint)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCENE 3 — CHAT
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawChatScene(canvas: Canvas) {
        val w = width.toFloat()
        var cursorY = dp(0f)

        // ── Header ──────────────────────────────────────────────────────────
        val headerH = dp(58f)
        fillPaint.color = colorBg
        canvas.drawRect(0f, 0f, w, headerH, fillPaint)
        strokePaint.color = Color.argb(25, 45, 106, 79)
        strokePaint.strokeWidth = dp(0.5f)
        strokePaint.style = Paint.Style.STROKE
        canvas.drawLine(0f, headerH, w, headerH, strokePaint)
        strokePaint.style = Paint.Style.FILL

        // Avatar circle
        val avR = dp(19f)
        val avCx = dp(20f) + avR; val avCy = headerH / 2f
        fillPaint.color = colorGreen
        canvas.drawCircle(avCx, avCy, avR, fillPaint)
        textPaint.color = Color.WHITE; textPaint.textSize = dp(12f)
        canvas.drawText("C", avCx, avCy + dp(4.5f), textPaint)

        textStartPaint.color = colorTextPri; textStartPaint.textSize = dp(13f)
        canvas.drawText("Calixy Coach", avCx + avR + dp(10f), avCy - dp(2f), textStartPaint)
        textStartPaint.color = colorGreenLight; textStartPaint.textSize = dp(10f)
        canvas.drawText("● Online now", avCx + avR + dp(10f), avCy + dp(12f), textStartPaint)

        cursorY = headerH + dp(14f)

        // ── Meal photo bubble ────────────────────────────────────────────────
        val photoAlpha  = (chatReveal / 0.2f).coerceIn(0f, 1f)
        val photoW      = dp(110f); val photoH = dp(66f)
        val photoRight  = w - dp(20f); val photoLeft = photoRight - photoW
        val photoRect   = RectF(photoLeft, cursorY, photoRight, cursorY + photoH)

        fillPaint.color = Color.argb((photoAlpha * 255).toInt().coerceIn(0,255), 197, 184, 168)
        canvas.drawRoundRect(photoRect, dp(14f), dp(14f), fillPaint)
        if (photoAlpha > 0.05f) {
            canvas.save()
            canvas.clipPath(Path().apply {
                addRoundRect(photoRect, dp(14f), dp(14f), Path.Direction.CW)
            })
            drawMealBowl(canvas, photoRect.centerX(), photoRect.centerY(), dp(30f), photoAlpha)
            canvas.restore()
        }

        cursorY += photoH + dp(8f)

        // ── User text bubble ─────────────────────────────────────────────────
        val userAlpha = ((chatReveal - 0.15f) / 0.2f).coerceIn(0f, 1f)
        val userText  = "Can I eat this after my workout?"
        textPaint.textSize = dp(13f)
        val userTextW = textPaint.measureText(userText)
        val ubW = userTextW + dp(28f); val ubH = dp(40f)
        val ubRight = w - dp(20f); val ubLeft = ubRight - ubW
        val ubRect = RectF(ubLeft, cursorY, ubRight, cursorY + ubH)
        fillPaint.color = Color.argb((userAlpha * 255).toInt().coerceIn(0,255), 45, 106, 79)
        canvas.drawRoundRect(ubRect, dp(18f), dp(18f), fillPaint)
        // Bottom-right sharp corner
        canvas.drawRect(ubRight - dp(14f), cursorY + ubH - dp(14f), ubRight, cursorY + ubH, fillPaint)
        textPaint.color = Color.argb((userAlpha * 255).toInt().coerceIn(0,255), 255, 255, 255)
        canvas.drawText(userText, ubRect.centerX(), ubRect.centerY() + dp(4.5f), textPaint)

        cursorY += ubH + dp(14f)

        // ── AI bubble ────────────────────────────────────────────────────────
        val aiAlpha = ((chatReveal - 0.3f) / 0.25f).coerceIn(0f, 1f)
        if (aiAlpha > 0f) {
            // Small avatar
            val smR = dp(14f); val smCx = dp(20f) + smR; val smCy = cursorY + smR
            fillPaint.color = Color.argb((aiAlpha * 255).toInt().coerceIn(0,255), 45, 106, 79)
            canvas.drawCircle(smCx, smCy, smR, fillPaint)
            textPaint.color = Color.argb((aiAlpha * 255).toInt().coerceIn(0,255), 255, 255, 255)
            textPaint.textSize = dp(9f)
            canvas.drawText("C", smCx, smCy + dp(3.5f), textPaint)

            val bubbleLeft  = smCx + smR + dp(8f)
            val bubbleRight = w - dp(16f)
            val bubbleW     = bubbleRight - bubbleLeft

            // Intro text
            textStartPaint.textSize  = dp(12.5f)
            textStartPaint.color     = Color.argb((aiAlpha * 255).toInt().coerceIn(0,255), 44, 62, 47)
            val introText = "Perfect post-workout choice. This bowl delivers exactly what your muscles need."
            val introH = drawWrappedText(null, introText, bubbleLeft, cursorY, bubbleW, dp(12.5f), dp(19f))

            // Nutrition card
            val cardTop   = cursorY + introH + dp(6f)
            val cardRect  = RectF(bubbleLeft, cardTop, bubbleRight, cardTop + dp(94f))
            fillPaint.color = Color.argb((aiAlpha * 255).toInt().coerceIn(0,255), 244, 250, 246)
            canvas.drawRoundRect(cardRect, dp(12f), dp(12f), fillPaint)
            strokePaint.color = Color.argb((aiAlpha * 60).toInt(), 45, 106, 79)
            strokePaint.strokeWidth = dp(0.5f)
            strokePaint.style = Paint.Style.STROKE
            canvas.drawRoundRect(cardRect, dp(12f), dp(12f), strokePaint)
            strokePaint.style = Paint.Style.FILL

            // Card header
            textStartPaint.textSize = dp(9.5f); textStartPaint.color = Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 45,106,79)
            canvas.drawText("NUTRITION BREAKDOWN", cardRect.left + dp(10f), cardRect.top + dp(18f), textStartPaint)
            // Score badge
            val badgeText = "Score 92/100"
            textPaint.textSize = dp(9f)
            val badgeW = textPaint.measureText(badgeText) + dp(14f)
            val badgeRect = RectF(cardRect.right - badgeW - dp(8f), cardRect.top + dp(8f), cardRect.right - dp(8f), cardRect.top + dp(22f))
            fillPaint.color = Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 45,106,79)
            fillPaint.alpha = (aiAlpha * 30).toInt().coerceIn(0,255)
            canvas.drawRoundRect(badgeRect, dp(8f), dp(8f), fillPaint)
            fillPaint.alpha = 255
            textPaint.color = Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 45,106,79)
            canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + dp(3.5f), textPaint)

            // 2x2 macro grid
            val macroVals  = arrayOf("487", "38g", "42g", "14g")
            val macroKeys  = arrayOf("calories", "protein", "carbs", "fats")
            val half       = bubbleW / 2f
            val gridTop    = cardRect.top + dp(28f)
            macroVals.forEachIndexed { i, v ->
                val col = i % 2; val row = i / 2
                val mx  = bubbleLeft + col * half + dp(8f)
                val my  = gridTop + row * dp(30f)
                textStartPaint.textSize = dp(15f)
                textStartPaint.color    = Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 26,46,31)
                canvas.drawText(v, mx, my, textStartPaint)
                textStartPaint.textSize = dp(9.5f)
                textStartPaint.color    = Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 122,154,126)
                canvas.drawText(macroKeys[i], mx, my + dp(12f), textStartPaint)
            }

            // Advice chips
            val adviceTop = cardRect.bottom + dp(8f)
            val advice1 = "Protein timing ideal — eat within 45 min of training."
            val advice2 = "Add fruit if you trained hard; avocado fats slow digestion slightly."
            drawAdviceChip(canvas, advice1, bubbleLeft, adviceTop, bubbleW, aiAlpha)
            val a1H = measureAdviceChipH(advice1, bubbleW) + dp(6f)
            drawAdviceChip(canvas, advice2, bubbleLeft, adviceTop + a1H, bubbleW, aiAlpha)

            // Intro text draw (on top)
            drawWrappedText(canvas, introText, bubbleLeft, cursorY, bubbleW, dp(12.5f), dp(19f),
                Color.argb((aiAlpha*255).toInt().coerceIn(0,255), 44, 62, 47))
        }

        // Forward arrow (scene 2 only — scene 3 has no next)
        // In scene 3 we omit the arrow; host will show the global "next" button
    }

    // ── Shared: meal bowl ─────────────────────────────────────────────────────
    private fun drawMealBowl(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Float) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val bW = radius * 2.1f; val bH = radius * 1.2f
        val bL = cx - bW / 2f; val bT = cy - bH * 0.3f
        val bowlRect = RectF(bL, bT, bL + bW, bT + bH)

        // Bowl body
        fillPaint.color = Color.argb(a, 107, 78, 61)
        canvas.drawOval(bowlRect, fillPaint)

        // Bowl rim
        val rimRect = RectF(bL + radius * 0.18f, bT - bH * 0.2f, bL + bW - radius * 0.18f, bT + bH * 0.25f)
        fillPaint.color = Color.argb(a, 139, 99, 85)
        canvas.drawOval(rimRect, fillPaint)

        // Protein slice
        fillPaint.color = Color.argb(a, 200, 168, 130)
        val pRect = RectF(cx - radius * 0.7f, bT - bH * 0.15f, cx + radius * 0.1f, bT + bH * 0.2f)
        canvas.drawRoundRect(pRect, dp(4f), dp(4f), fillPaint)

        // Greens
        fillPaint.color = Color.argb(a, 74, 124, 89)
        canvas.drawOval(RectF(cx + radius * 0.1f, bT - bH * 0.28f, cx + radius * 0.7f, bT + bH * 0.1f), fillPaint)

        // Grain layer
        fillPaint.color = Color.argb((a * 0.8f).toInt(), 212, 180, 131)
        canvas.drawRoundRect(RectF(bL + bW * 0.25f, bT + bH * 0.18f, bL + bW * 0.75f, bT + bH * 0.38f), dp(3f), dp(3f), fillPaint)
    }

    // ── Shared: forward arrow ─────────────────────────────────────────────────
    private fun drawForwardArrow(canvas: Canvas) {
        val ab = arrowRect()
        fillPaint.color = colorGreen
        canvas.drawCircle(ab.centerX(), ab.centerY(), ab.width() / 2f, fillPaint)
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = dp(2f)
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.style = Paint.Style.STROKE
        val mx = ab.centerX(); val my = ab.centerY()
        canvas.drawLine(mx - dp(5f), my - dp(5f), mx + dp(4f), my, strokePaint)
        canvas.drawLine(mx + dp(4f), my, mx - dp(5f), my + dp(5f), strokePaint)
        strokePaint.style = Paint.Style.FILL
    }

    // ── Text utilities ────────────────────────────────────────────────────────
    private fun drawWrappedText(
        canvas: Canvas?, text: String, x: Float, y: Float,
        maxW: Float, textSizePx: Float, lineH: Float,
        color: Int = Color.BLACK
    ): Float {
        textStartPaint.textSize = textSizePx
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line  = ""
        for (w2 in words) {
            val test = if (line.isEmpty()) w2 else "$line $w2"
            if (textStartPaint.measureText(test) <= maxW) line = test
            else { if (line.isNotEmpty()) lines.add(line); line = w2 }
        }
        if (line.isNotEmpty()) lines.add(line)
        if (canvas != null) {
            textStartPaint.color = color
            lines.forEachIndexed { i, l -> canvas.drawText(l, x, y + lineH * (i + 1), textStartPaint) }
        }
        return lineH * lines.size
    }

    private fun measureAdviceChipH(text: String, maxW: Float): Float {
        textStartPaint.textSize = dp(11f)
        val words = text.split(" ")
        var lines = 1; var line = ""
        for (w2 in words) {
            val test = if (line.isEmpty()) w2 else "$line $w2"
            if (textStartPaint.measureText(test) <= maxW - dp(20f)) line = test
            else { lines++; line = w2 }
        }
        return dp(14f) + lines * dp(16f) + dp(10f)
    }

    private fun drawAdviceChip(canvas: Canvas, text: String, x: Float, y: Float, maxW: Float, alpha: Float) {
        val h = measureAdviceChipH(text, maxW)
        val rect = RectF(x, y, x + maxW, y + h)
        fillPaint.color = Color.argb((alpha * 18).toInt().coerceIn(0,255), 45, 106, 79)
        canvas.drawRoundRect(rect, dp(8f), dp(8f), fillPaint)
        // Left accent bar
        fillPaint.color = Color.argb((alpha * 80).toInt().coerceIn(0,255), 45, 106, 79)
        canvas.drawRoundRect(RectF(x, y, x + dp(2.5f), y + h), dp(1.5f), dp(1.5f), fillPaint)
        // Text
        drawWrappedText(canvas, text, x + dp(10f), y, maxW - dp(20f), dp(11f), dp(16f),
            Color.argb((alpha * 255).toInt().coerceIn(0,255), 62, 107, 79))
    }
}