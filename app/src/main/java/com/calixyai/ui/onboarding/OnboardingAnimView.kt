package com.calixyai.ui.onboarding

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class OnboardingAnimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var pageIndex: Int = 0
        set(value) {
            field = value
            barProgress = 0f
            chatStep = 0
            tick = 0f
            // Reset page-2 state
            p2Phase = Phase2.CAMERA
            p2PhaseTimer = 0f
            p2CardReveal = 0f
            p2ScanRadius = 0f
            p2CaptureFlash = 0f
            p2ChatStep = 0
            p2ChatStepTimer = 0f
            p2MacroProgress = 0f
            p2Particles.clear()
            p2ParticlesInit = false
            invalidate()
        }

    // ── Page 2 enums & state ──────────────────────────────────────────────────

    private enum class Phase2 {
        CAMERA,       // phone + food + focus ring
        FLASH,        // white flash capture moment
        AI_SCAN,      // ripples + AI orb
        CHAT,         // chat bubbles appear
        CARD          // nutrition card slides up
    }

    private var p2Phase = Phase2.CAMERA
    private var p2PhaseTimer = 0f          // ticks since phase start
    private var p2CardReveal = 0f          // 0..1 card slide progress
    private var p2ScanRadius = 0f          // ripple expansion
    private var p2CaptureFlash = 0f        // 0..1 flash brightness
    private var p2ChatStep = 0             // how many chat bubbles shown
    private var p2ChatStepTimer = 0f
    private var p2MacroProgress = 0f       // 0..1 macro bars
    private var p2ParticlesInit = false

    private data class P2Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var alpha: Float, var r: Float, var life: Float, var maxLife: Float
    )
    private val p2Particles = mutableListOf<P2Particle>()

    // Phase durations in ticks (60fps target ≈ 16ms/frame)
    private val CAMERA_TICKS = 120f
    private val FLASH_TICKS  = 18f
    private val SCAN_TICKS   = 100f
    private val CHAT_TICKS   = 160f

    // ── Shared ────────────────────────────────────────────────────────────────
    private var tick = 0f

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 0 — PREMIUM ORB (unchanged)
    // ══════════════════════════════════════════════════════════════════════════

    private var orbFloatY = 0f
    private var orbFloatPhase = 0f
    private var orbTapScale = 1f
    private var orbTapDecay = 0f
    private var breathPhase = 0f
    private var blinkPhase = 0f
    private var blinkTimer = 0f
    private var blinkInterval = 180f
    private var eyeLeftScale = 1f
    private var eyeRightScale = 1f
    private var isWink = false
    private val orbIcons = listOf("🏋️", "🌙", "🧘", "🥗", "💚")
    private var orbitAngle = 0f
    private val iconPhases = orbIcons.mapIndexed { i, _ -> (i.toFloat() / orbIcons.size) * 2f * PI.toFloat() }
    private val iconFloatPhases = orbIcons.map { (Math.random() * 2 * PI).toFloat() }

    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float, var r: Float, var phase: Float)
    private val particles = mutableListOf<Particle>()
    private var particlesInit = false

    private val orbPaint       = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.8f }
    private val eyePaint       = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smilePaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3.2f; strokeCap = Paint.Cap.ROUND }
    private val shadowPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconBgPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconStrokePaint= Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f }
    private val iconTextPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val particlePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── PAGE 1: Bars ──────────────────────────────────────────────────────────
    private data class Bar(val label: String, val target: Float, val color: Int)
    private val bars = listOf(
        Bar("Protein", 0.78f, Color.parseColor("#4CAF50")),
        Bar("Carbs",   0.52f, Color.parseColor("#66BB6A")),
        Bar("Fats",    0.35f, Color.parseColor("#A5D6A7")),
        Bar("Fiber",   0.61f, Color.parseColor("#2E7D32")),
    )
    private var barProgress = 0f
    private val paintBar   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBarBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80FFFFFF"); textAlign = Paint.Align.CENTER; textSize = 28f }
    private val paintValue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E8F5E9"); textAlign = Paint.Align.CENTER; textSize = 30f; isFakeBoldText = true }

    // ── PAGE 2: Chat (legacy, now replaced by cinematic) ─────────────────────
    private data class Bubble(val text: String, val isUser: Boolean)
    private val bubbles = listOf(
        Bubble("What should I eat today?", true),
        Bubble("Try high-protein breakfast — eggs + avocado 🥑", false),
        Bubble("Track lunch: grilled chicken", true),
        Bubble("Added! 340 kcal · 38g protein ✓", false),
    )
    private var chatStep = 0

    // ── PAGE 2 paints (cinematic) ─────────────────────────────────────────────
    private val p2Paint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val p2TextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val p2TextL     = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val p2TextR     = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val p2Stroke    = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val p2Fill      = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val GREEN       = Color.parseColor("#0DBF85")
    private val GREEN_DARK  = Color.parseColor("#0A9A6A")
    private val GREEN_LIGHT = Color.parseColor("#E1F9F0")
    private val CREAM       = Color.parseColor("#FAF7F2")
    private val CARD_BG     = Color.parseColor("#FFFFFF")
    private val TEXT_PRI    = Color.parseColor("#1A2E1A")
    private val TEXT_MUT    = Color.parseColor("#6B7B6B")

    // 3D projection cache (page 0)
    private data class Icon3D(val screenX: Float, val screenY: Float, val depth: Float, val idx: Int)
    private var cachedProjected: List<Icon3D> = emptyList()
    private var cachedProjectedTick = -1f

    // ── Animation loop ────────────────────────────────────────────────────────
    private val frameRunnable = object : Runnable {
        override fun run() {
            tick += 1f
            when (pageIndex) {
                0 -> tickOrb()
                1 -> if (barProgress < 1f) barProgress = min(1f, barProgress + 0.022f)
                2 -> tickPage2()
            }
            invalidate()
            postDelayed(this, 16L)
        }
    }

    private fun tickOrb() {
        breathPhase  += 0.018f
        orbFloatPhase += 0.012f
        orbFloatY     = sin(orbFloatPhase) * 10f
        orbitAngle   += 0.008f
        if (orbTapDecay > 0f) {
            orbTapDecay = max(0f, orbTapDecay - 0.06f)
            orbTapScale = 1f + orbTapDecay * 0.12f
        }
        blinkTimer += 1f
        if (blinkTimer >= blinkInterval) {
            blinkTimer    = 0f
            blinkInterval = 120f + (Math.random() * 220f).toFloat()
            isWink        = Math.random() < 0.28
            blinkPhase    = 0f
        }
        if (blinkPhase < 1f) {
            blinkPhase   = min(1f, blinkPhase + 0.14f)
            val blink    = sin(blinkPhase * PI.toFloat())
            eyeLeftScale  = 1f - blink * 0.92f
            eyeRightScale = if (isWink) 1f else 1f - blink * 0.92f
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE 2 TICK
    // ─────────────────────────────────────────────────────────────────────────
    private fun tickPage2() {
        p2PhaseTimer += 1f

        when (p2Phase) {
            Phase2.CAMERA -> {
                if (p2PhaseTimer >= CAMERA_TICKS) advancePage2Phase()
            }
            Phase2.FLASH -> {
                p2CaptureFlash = 1f - (p2PhaseTimer / FLASH_TICKS)
                if (p2PhaseTimer >= FLASH_TICKS) advancePage2Phase()
            }
            Phase2.AI_SCAN -> {
                p2ScanRadius = min(1f, p2PhaseTimer / SCAN_TICKS)
                spawnScanParticles()
                tickP2Particles()
                if (p2PhaseTimer >= SCAN_TICKS) advancePage2Phase()
            }
            Phase2.CHAT -> {
                tickP2Particles()
                p2ChatStepTimer += 1f
                if (p2ChatStepTimer > 38f && p2ChatStep < 4) {
                    p2ChatStep++
                    p2ChatStepTimer = 0f
                }
                if (p2PhaseTimer >= CHAT_TICKS) advancePage2Phase()
            }
            Phase2.CARD -> {
                p2CardReveal   = min(1f, p2CardReveal + 0.025f)
                p2MacroProgress = min(1f, p2MacroProgress + 0.018f)
                // Loop back after enough time
                if (p2PhaseTimer > 260f) {
                    p2Phase = Phase2.CAMERA
                    p2PhaseTimer   = 0f
                    p2CardReveal   = 0f
                    p2ScanRadius   = 0f
                    p2CaptureFlash = 0f
                    p2ChatStep     = 0
                    p2ChatStepTimer = 0f
                    p2MacroProgress = 0f
                    p2Particles.clear()
                }
            }
        }
    }

    private fun advancePage2Phase() {
        p2Phase = when (p2Phase) {
            Phase2.CAMERA  -> Phase2.FLASH
            Phase2.FLASH   -> Phase2.AI_SCAN
            Phase2.AI_SCAN -> Phase2.CHAT
            Phase2.CHAT    -> Phase2.CARD
            Phase2.CARD    -> Phase2.CAMERA
        }
        p2PhaseTimer = 0f
    }

    private fun spawnScanParticles() {
        if (p2Particles.size >= 30) return
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h * 0.38f
        repeat(2) {
            val angle = (Math.random() * 2 * PI).toFloat()
            val speed = (1.5f + Math.random().toFloat() * 2.5f)
            p2Particles.add(P2Particle(
                x = cx, y = cy,
                vx = cos(angle) * speed, vy = sin(angle) * speed,
                alpha = 0.9f, r = (2f + Math.random().toFloat() * 3f),
                life = 0f, maxLife = 40f + Math.random().toFloat() * 30f
            ))
        }
    }

    private fun tickP2Particles() {
        p2Particles.removeAll { it.life >= it.maxLife }
        p2Particles.forEach { p ->
            p.x    += p.vx
            p.y    += p.vy
            p.vy   += 0.04f
            p.life += 1f
            p.alpha = 1f - (p.life / p.maxLife)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ATTACH / DETACH / TOUCH
    // ══════════════════════════════════════════════════════════════════════════

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(frameRunnable) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); removeCallbacks(frameRunnable) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (pageIndex == 0 && event.action == MotionEvent.ACTION_DOWN) {
            val cx = width / 2f; val cy = height * 0.42f + orbFloatY
            val dx = event.x - cx; val dy = event.y - cy
            val orbR = min(width, height) * 0.25f
            if (dx * dx + dy * dy < orbR * orbR * 1.4f) {
                orbTapDecay = 1f; isWink = true; blinkPhase = 0f; blinkTimer = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // onDraw
    // ══════════════════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (pageIndex) {
            0 -> drawPremiumOrb(canvas)
            1 -> drawBars(canvas)
            2 -> drawPage2Cinematic(canvas)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 2 — CINEMATIC ONBOARDING
    // ══════════════════════════════════════════════════════════════════════════

    private fun drawPage2Cinematic(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        when (p2Phase) {
            Phase2.CAMERA  -> drawCamera(canvas, w, h)
            Phase2.FLASH   -> { drawCamera(canvas, w, h); drawFlash(canvas, w, h) }
            Phase2.AI_SCAN -> drawAiScan(canvas, w, h)
            Phase2.CHAT    -> drawChatScene(canvas, w, h)
            Phase2.CARD    -> drawNutritionCard(canvas, w, h)
        }
    }

    // ─── CAMERA ──────────────────────────────────────────────────────────────
    private fun drawCamera(canvas: Canvas, w: Float, h: Float) {
        val dp = resources.displayMetrics.density
        val cx = w / 2f
        val floatY = sin(tick * 0.04f) * dp * 5f

        // Phone body
        val phoneW = dp * 130f; val phoneH = dp * 210f
        val phoneLeft = cx - phoneW / 2f
        val phoneTop  = h * 0.08f + floatY
        p2Fill.color = Color.parseColor("#111111")
        p2Fill.setShadowLayer(dp * 18f, 0f, dp * 8f, Color.argb(50, 0, 0, 0))
        canvas.drawRoundRect(
            RectF(phoneLeft, phoneTop, phoneLeft + phoneW, phoneTop + phoneH),
            dp * 22f, dp * 22f, p2Fill
        )
        p2Fill.clearShadowLayer()

        // Screen
        val scrPad = dp * 5f
        val scrL = phoneLeft + scrPad; val scrT = phoneTop + scrPad
        val scrR = phoneLeft + phoneW - scrPad; val scrB = phoneTop + phoneH - scrPad
        p2Fill.color = Color.parseColor("#0D1A0D")
        canvas.drawRoundRect(RectF(scrL, scrT, scrR, scrB), dp * 18f, dp * 18f, p2Fill)

        // Notch
        p2Fill.color = Color.parseColor("#111111")
        canvas.drawRoundRect(
            RectF(cx - dp * 22f, scrT, cx + dp * 22f, scrT + dp * 16f),
            dp * 8f, dp * 8f, p2Fill
        )

        // Camera label
        p2TextPaint.color = Color.argb(120, 255, 255, 255)
        p2TextPaint.textSize = dp * 8f
        p2TextPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PHOTO", cx, scrT + dp * 26f, p2TextPaint)

        // Grid lines
        p2Stroke.color = Color.argb(30, 255, 255, 255); p2Stroke.strokeWidth = 0.5f
        val colW = (scrR - scrL) / 3f; val rowH = (scrB - scrT) / 3f
        for (i in 1..2) {
            canvas.drawLine(scrL + colW * i, scrT, scrL + colW * i, scrB, p2Stroke)
            canvas.drawLine(scrL, scrT + rowH * i, scrR, scrT + rowH * i, p2Stroke)
        }

        // Food emoji in center
        val foodX = cx; val foodY = (scrT + scrB) / 2f + dp * 4f + sin(tick * 0.05f) * dp * 3f
        p2TextPaint.textSize = dp * 42f
        canvas.drawText("🥗", foodX, foodY, p2TextPaint)

        // Scan line
        val scanProgress = ((tick * 0.012f) % 1f)
        val scanY = scrT + dp * 20f + (scrB - scrT - dp * 24f) * scanProgress
        val scanGrad = LinearGradient(scrL, scanY, scrR, scanY,
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 255, 215, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        p2Fill.shader = scanGrad
        canvas.drawRect(scrL, scanY - dp * 1f, scrR, scanY + dp * 1f, p2Fill)
        p2Fill.shader = null

        // Focus ring (animated pulse)
        val focusPulse = 0.9f + 0.1f * sin(tick * 0.12f)
        val focusSize = dp * 44f * focusPulse
        val focusCx = cx; val focusCy = (scrT + scrB) / 2f
        p2Stroke.color = Color.argb(200, 255, 215, 0)
        p2Stroke.strokeWidth = dp * 1.5f
        canvas.drawRoundRect(
            RectF(focusCx - focusSize, focusCy - focusSize, focusCx + focusSize, focusCy + focusSize),
            dp * 3f, dp * 3f, p2Stroke
        )
        // Corners
        val cLen = dp * 12f; val co = -focusSize
        listOf(
            Pair(focusCx + co, focusCy + co) to Pair(focusCx + co + cLen, focusCy + co),
            Pair(focusCx - co, focusCy + co) to Pair(focusCx - co - cLen, focusCy + co),
            Pair(focusCx + co, focusCy - co) to Pair(focusCx + co + cLen, focusCy - co),
            Pair(focusCx - co, focusCy - co) to Pair(focusCx - co - cLen, focusCy - co),
        ).forEach { (a, b) ->
            p2Stroke.color = Color.argb(255, 255, 215, 0); p2Stroke.strokeWidth = dp * 2f
            canvas.drawLine(a.first, a.second, b.first, b.second, p2Stroke)
        }

        // Bottom UI bar
        p2Fill.color = Color.argb(160, 0, 0, 0)
        canvas.drawRoundRect(RectF(scrL, scrB - dp * 40f, scrR, scrB), dp * 18f, dp * 18f, p2Fill)
        // Shutter
        p2Fill.color = Color.WHITE
        canvas.drawCircle(cx, scrB - dp * 20f, dp * 14f, p2Fill)
        p2Stroke.color = Color.argb(120, 255, 255, 255); p2Stroke.strokeWidth = dp * 2f
        canvas.drawCircle(cx, scrB - dp * 20f, dp * 17f, p2Stroke)

        // Caption below phone
        p2TextPaint.color = Color.parseColor("#6B7B6B")
        p2TextPaint.textSize = dp * 10f
        p2TextPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Point camera at your meal", cx, phoneTop + phoneH + dp * 22f, p2TextPaint)
    }

    private fun drawFlash(canvas: Canvas, w: Float, h: Float) {
        val alpha = (p2CaptureFlash * 255).toInt().coerceIn(0, 255)
        p2Fill.color = Color.argb(alpha, 255, 255, 255)
        canvas.drawRect(0f, 0f, w, h, p2Fill)
    }

    // ─── AI SCAN ─────────────────────────────────────────────────────────────
    private fun drawAiScan(canvas: Canvas, w: Float, h: Float) {
        val dp  = resources.displayMetrics.density
        val cx  = w / 2f; val cy = h * 0.38f

        // Ambient bg glow
        val ambGrad = RadialGradient(cx, cy, w * 0.55f,
            intArrayOf(Color.argb(30, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        p2Fill.shader = ambGrad
        canvas.drawRect(0f, 0f, w, h, p2Fill)
        p2Fill.shader = null

        // Ripple rings
        val maxRipple = w * 0.46f
        for (i in 0..2) {
            val delay = i / 3f
            val t = ((p2ScanRadius - delay).coerceIn(0f, 1f))
            if (t <= 0f) continue
            val radius = maxRipple * t
            val alpha = ((1f - t) * 120).toInt()
            p2Stroke.color = Color.argb(alpha, 13, 191, 133)
            p2Stroke.strokeWidth = dp * 1.5f
            canvas.drawCircle(cx, cy, radius, p2Stroke)
        }

        // Particles
        p2Particles.forEach { p ->
            val a = (p.alpha * 200).toInt().coerceIn(0, 200)
            particlePaint.color = Color.argb(a, 13, 191, 133)
            canvas.drawCircle(p.x, p.y, p.r, particlePaint)
        }

        // Center food orb
        val orbR = dp * 40f
        val orbGlow = RadialGradient(cx, cy, orbR * 1.8f,
            intArrayOf(Color.argb(60, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        p2Fill.shader = orbGlow
        canvas.drawCircle(cx, cy, orbR * 1.8f, p2Fill)
        p2Fill.shader = null

        p2Fill.color = GREEN_LIGHT
        canvas.drawCircle(cx, cy, orbR, p2Fill)
        p2Stroke.color = Color.argb(80, 13, 191, 133); p2Stroke.strokeWidth = dp * 1.5f
        canvas.drawCircle(cx, cy, orbR, p2Stroke)

        p2TextPaint.textSize = dp * 30f
        canvas.drawText("🥗", cx, cy + dp * 11f, p2TextPaint)

        // Orbiting AI dot
        val aiAngle = tick * 0.06f
        val aiOrbitR = orbR * 1.9f
        val aiX = cx + cos(aiAngle) * aiOrbitR
        val aiY = cy + sin(aiAngle) * aiOrbitR
        p2Fill.color = GREEN
        p2Fill.setShadowLayer(dp * 8f, 0f, 0f, Color.argb(90, 13, 191, 133))
        canvas.drawCircle(aiX, aiY, dp * 12f, p2Fill)
        p2Fill.clearShadowLayer()
        p2TextPaint.textSize = dp * 13f
        canvas.drawText("🤖", aiX, aiY + dp * 5f, p2TextPaint)

        // Chips
        val chipY0 = cy + orbR + dp * 28f
        val chips = listOf("Scanning ingredients…", "Calculating macros…", "Generating insights…")
        val visibleChips = ((p2PhaseTimer / SCAN_TICKS) * 3f).toInt().coerceIn(0, 3)
        chips.take(visibleChips).forEachIndexed { i, text ->
            val chipY = chipY0 + i * dp * 30f
            val chipAlpha = min(1f, (p2PhaseTimer - i * 30f) / 18f)
            val ca = (chipAlpha * 255).toInt().coerceIn(0, 255)
            p2Fill.color = Color.argb(min(ca, 220), 225, 249, 240)
            val chipW = dp * 160f; val chipH = dp * 22f
            canvas.drawRoundRect(
                RectF(cx - chipW / 2f, chipY - chipH / 2f, cx + chipW / 2f, chipY + chipH / 2f),
                chipH / 2f, chipH / 2f, p2Fill
            )
            // Pulse dot
            val dotA = (sin(tick * 0.2f + i) * 0.4f + 0.6f)
            p2Fill.color = Color.argb((dotA * ca).toInt().coerceIn(0, 255), 13, 191, 133)
            canvas.drawCircle(cx - chipW / 2f + dp * 12f, chipY, dp * 3.5f, p2Fill)

            p2TextPaint.color = Color.argb(ca, 10, 154, 106)
            p2TextPaint.textSize = dp * 10f
            p2TextPaint.typeface = Typeface.DEFAULT
            canvas.drawText(text, cx + dp * 6f, chipY + dp * 3.5f, p2TextPaint)
        }

        // Loading bar
        val barW = dp * 160f; val barH = dp * 3f
        val barY = chipY0 + chips.size * dp * 30f + dp * 8f
        p2Fill.color = Color.argb(40, 13, 191, 133)
        canvas.drawRoundRect(RectF(cx - barW/2f, barY, cx + barW/2f, barY + barH), barH/2f, barH/2f, p2Fill)
        val shimmer = (tick * 0.018f) % 1f
        val shimGrad = LinearGradient(cx - barW/2f, 0f, cx + barW/2f, 0f,
            intArrayOf(Color.argb(80, 13,191,133), GREEN, Color.argb(80, 13,191,133)),
            floatArrayOf(max(0f, shimmer - 0.3f), shimmer, min(1f, shimmer + 0.3f)),
            Shader.TileMode.CLAMP)
        p2Fill.shader = shimGrad
        canvas.drawRoundRect(RectF(cx - barW/2f, barY, cx + barW/2f, barY + barH), barH/2f, barH/2f, p2Fill)
        p2Fill.shader = null
    }

    // ─── CHAT ─────────────────────────────────────────────────────────────────
    private fun drawChatScene(canvas: Canvas, w: Float, h: Float) {
        val dp = resources.displayMetrics.density

        // Chat card background
        val cardL  = dp * 16f; val cardR = w - dp * 16f
        val cardT  = dp * 10f; val cardB = h - dp * 10f
        p2Fill.color = CARD_BG
        p2Fill.setShadowLayer(dp * 12f, 0f, dp * 4f, Color.argb(20, 0, 0, 0))
        canvas.drawRoundRect(RectF(cardL, cardT, cardR, cardB), dp * 20f, dp * 20f, p2Fill)
        p2Fill.clearShadowLayer()

        // Header
        val headerH = dp * 52f
        p2Fill.color = CARD_BG
        canvas.drawRoundRect(RectF(cardL, cardT, cardR, cardT + headerH + dp * 10f), dp * 20f, dp * 20f, p2Fill)
        p2Stroke.color = Color.argb(30, 13, 191, 133); p2Stroke.strokeWidth = dp * 0.5f
        canvas.drawLine(cardL + dp * 12f, cardT + headerH, cardR - dp * 12f, cardT + headerH, p2Stroke)

        // Avatar
        val avCx = cardL + dp * 32f; val avCy = cardT + headerH / 2f + dp * 2f
        val avR  = dp * 17f
        p2Fill.color = GREEN
        canvas.drawCircle(avCx, avCy, avR, p2Fill)
        p2TextPaint.textSize = dp * 14f
        canvas.drawText("🤖", avCx, avCy + dp * 5f, p2TextPaint)
        // Online dot
        p2Fill.color = GREEN
        canvas.drawCircle(avCx + avR * 0.68f, avCy + avR * 0.68f, dp * 4.5f, p2Fill)
        p2Fill.color = CARD_BG
        canvas.drawCircle(avCx + avR * 0.68f, avCy + avR * 0.68f, dp * 3f, p2Fill)
        p2Fill.color = GREEN
        canvas.drawCircle(avCx + avR * 0.68f, avCy + avR * 0.68f, dp * 2.5f, p2Fill)

        p2TextL.color = TEXT_PRI; p2TextL.textSize = dp * 12f; p2TextL.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Calixy", avCx + avR + dp * 8f, avCy - dp * 3f, p2TextL)
        p2TextL.color = GREEN; p2TextL.textSize = dp * 9f; p2TextL.typeface = Typeface.DEFAULT
        canvas.drawText("● Active now", avCx + avR + dp * 8f, avCy + dp * 8f, p2TextL)

        // Messages
        var msgY = cardT + headerH + dp * 16f

        // User: food photo bubble
        if (p2ChatStep >= 1) {
            val bubW = dp * 120f; val bubH = dp * 38f
            val bx = cardR - dp * 14f - bubW; val by = msgY
            p2Fill.color = CARD_BG
            p2Fill.setShadowLayer(dp * 4f, 0f, dp * 2f, Color.argb(12, 0, 0, 0))
            canvas.drawRoundRect(RectF(bx, by, bx + bubW, by + bubH), dp * 12f, dp * 12f, p2Fill)
            p2Fill.clearShadowLayer()
            // Thumb
            p2Fill.color = GREEN_LIGHT
            canvas.drawRoundRect(RectF(bx + dp * 6f, by + dp * 6f, bx + dp * 28f, by + bubH - dp * 6f), dp * 6f, dp * 6f, p2Fill)
            p2TextL.textSize = dp * 16f
            canvas.drawText("🥗", bx + dp * 17f, by + bubH / 2f + dp * 6f, p2TextL)
            p2TextL.color = TEXT_PRI; p2TextL.textSize = dp * 9.5f; p2TextL.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Salad Bowl.jpg", bx + dp * 34f, by + bubH / 2f - dp * 2f, p2TextL)
            p2TextL.color = GREEN; p2TextL.textSize = dp * 8.5f; p2TextL.typeface = Typeface.DEFAULT
            canvas.drawText("Analyze this →", bx + dp * 34f, by + bubH / 2f + dp * 9f, p2TextL)
            msgY += bubH + dp * 10f
        }

        // Bot typing then message
        if (p2ChatStep == 2) {
            msgY = drawTypingBubble(canvas, dp, cardL + dp * 14f, msgY)
        }
        if (p2ChatStep >= 3) {
            val msg = "I've analysed your meal! 🌿\nDetected: Grilled chicken, mixed greens,\ncucumber, tomato & olive oil dressing."
            msgY = drawBotBubble(canvas, dp, cardL + dp * 14f, cardR - dp * 14f, msgY, msg)
        }
        if (p2ChatStep >= 4) {
            val msg = "Nutrition score: 92/100 ✨\nGenerating your full breakdown…"
            drawBotBubble(canvas, dp, cardL + dp * 14f, cardR - dp * 14f, msgY + dp * 6f, msg, highlight = true)
        }
    }

    private fun drawTypingBubble(canvas: Canvas, dp: Float, x: Float, y: Float): Float {
        val bH = dp * 28f; val bW = dp * 54f
        p2Fill.color = CARD_BG
        p2Fill.setShadowLayer(dp * 3f, 0f, dp * 1.5f, Color.argb(10, 0, 0, 0))
        canvas.drawRoundRect(RectF(x, y, x + bW, y + bH), dp * 10f, dp * 10f, p2Fill)
        p2Fill.clearShadowLayer()
        for (i in 0..2) {
            val dotX = x + dp * 12f + i * dp * 14f
            val dotY = y + bH / 2f + sin(tick * 0.18f + i * 1.1f) * dp * 3.5f
            val da = (sin(tick * 0.18f + i * 1.1f) * 0.3f + 0.7f)
            p2Fill.color = Color.argb((da * 255).toInt(), 13, 191, 133)
            canvas.drawCircle(dotX, dotY, dp * 4f, p2Fill)
        }
        return y + bH
    }

    private fun drawBotBubble(
        canvas: Canvas, dp: Float,
        x: Float, maxX: Float, y: Float,
        text: String, highlight: Boolean = false
    ): Float {
        val bubW = maxX - x - dp * 20f
        val lines = text.split("\n")
        val lineH = dp * 14f
        val padV  = dp * 10f; val padH = dp * 12f
        val bH = padV * 2f + lines.size * lineH

        p2Fill.color = if (highlight) Color.argb(30, 13, 191, 133) else CARD_BG
        p2Fill.setShadowLayer(dp * 4f, 0f, dp * 2f, Color.argb(10, 0, 0, 0))
        canvas.drawRoundRect(RectF(x, y, x + bubW, y + bH), dp * 12f, dp * 12f, p2Fill)
        p2Fill.clearShadowLayer()
        if (highlight) {
            p2Stroke.color = Color.argb(60, 13, 191, 133); p2Stroke.strokeWidth = dp * 1f
            canvas.drawRoundRect(RectF(x, y, x + bubW, y + bH), dp * 12f, dp * 12f, p2Stroke)
        }

        lines.forEachIndexed { i, line ->
            val ty = y + padV + (i + 1) * lineH - dp * 2f
            p2TextL.color = if (highlight && i == 0) GREEN_DARK else TEXT_PRI
            p2TextL.textSize = dp * 10f
            p2TextL.typeface = if (highlight && i == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(line, x + padH, ty, p2TextL)
        }
        return y + bH
    }

    // ─── NUTRITION CARD ───────────────────────────────────────────────────────
    private fun drawNutritionCard(canvas: Canvas, w: Float, h: Float) {
        val dp = resources.displayMetrics.density
        val cx = w / 2f

        // Slide-up reveal
        val slideOffset = (1f - easeOut(p2CardReveal)) * h * 0.4f

        canvas.save()
        canvas.translate(0f, slideOffset)

        val cardL = dp * 14f; val cardR = w - dp * 14f; val cardW = cardR - cardL
        val cardT = dp * 8f

        // Card shadow + bg
        p2Fill.color = CARD_BG
        p2Fill.setShadowLayer(dp * 16f, 0f, dp * 6f, Color.argb(30, 13, 191, 133))
        canvas.drawRoundRect(RectF(cardL, cardT, cardR, cardT + h - dp * 16f), dp * 22f, dp * 22f, p2Fill)
        p2Fill.clearShadowLayer()

        var y = cardT + dp * 20f

        // Header row
        p2TextL.color = TEXT_PRI; p2TextL.textSize = dp * 13f; p2TextL.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Grilled Chicken Salad", cardL + dp * 16f, y, p2TextL)
        // Score badge
        val scoreR = dp * 22f; val scoreCx = cardR - dp * 16f - scoreR; val scoreCy = y - dp * 4f
        p2Fill.color = GREEN
        canvas.drawCircle(scoreCx, scoreCy, scoreR, p2Fill)
        p2TextPaint.color = Color.WHITE; p2TextPaint.textSize = dp * 13f; p2TextPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("92", scoreCx, scoreCy + dp * 2f, p2TextPaint)
        p2TextPaint.textSize = dp * 7f; p2TextPaint.typeface = Typeface.DEFAULT
        canvas.drawText("score", scoreCx, scoreCy + dp * 12f, p2TextPaint)

        y += dp * 8f
        p2TextL.color = TEXT_MUT; p2TextL.textSize = dp * 9.5f; p2TextL.typeface = Typeface.DEFAULT
        canvas.drawText("High protein · Mediterranean style", cardL + dp * 16f, y, p2TextL)
        y += dp * 14f

        // Calorie strip
        p2Fill.color = GREEN_LIGHT
        canvas.drawRoundRect(RectF(cardL + dp * 12f, y, cardR - dp * 12f, y + dp * 36f), dp * 12f, dp * 12f, p2Fill)
        p2TextL.color = GREEN_DARK; p2TextL.textSize = dp * 22f; p2TextL.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("487", cardL + dp * 26f, y + dp * 26f, p2TextL)
        p2TextL.color = TEXT_MUT; p2TextL.textSize = dp * 9.5f; p2TextL.typeface = Typeface.DEFAULT
        canvas.drawText("kcal per serving", cardL + dp * 68f, y + dp * 22f, p2TextL)
        // On track badge
        val badgeW = dp * 58f; val badgeH = dp * 18f
        val badgeX = cardR - dp * 20f - badgeW; val badgeY = y + (dp * 36f - badgeH) / 2f
        p2Fill.color = GREEN
        canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH), badgeH/2f, badgeH/2f, p2Fill)
        p2TextPaint.color = Color.WHITE; p2TextPaint.textSize = dp * 8.5f; p2TextPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("✓ On track", badgeX + badgeW/2f, badgeY + dp * 12.5f, p2TextPaint)
        y += dp * 46f

        // Macro grid (2x2)
        val macros = listOf(
            Triple("Protein", "42g", 0.84f),
            Triple("Carbs",   "31g", 0.48f),
            Triple("Fats",    "18g", 0.36f),
            Triple("Fiber",   "6g",  0.55f)
        )
        val cellW = (cardW - dp * 36f) / 2f; val cellH = dp * 52f; val cellGap = dp * 8f
        macros.forEachIndexed { i, (label, value, target) ->
            val col = i % 2; val row = i / 2
            val cx2 = cardL + dp * 12f + col * (cellW + cellGap)
            val cy2 = y + row * (cellH + cellGap)
            p2Fill.color = GREEN_LIGHT
            canvas.drawRoundRect(RectF(cx2, cy2, cx2 + cellW, cy2 + cellH), dp * 10f, dp * 10f, p2Fill)
            p2TextL.color = TEXT_MUT; p2TextL.textSize = dp * 8.5f; p2TextL.typeface = Typeface.DEFAULT
            canvas.drawText(label.uppercase(), cx2 + dp * 8f, cy2 + dp * 14f, p2TextL)
            p2TextL.color = GREEN_DARK; p2TextL.textSize = dp * 18f; p2TextL.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(value, cx2 + dp * 8f, cy2 + dp * 34f, p2TextL)
            // Progress bar
            val barW = cellW - dp * 16f; val barH2 = dp * 3f
            val barY2 = cy2 + cellH - dp * 9f
            p2Fill.color = Color.argb(40, 13, 191, 133)
            canvas.drawRoundRect(RectF(cx2 + dp * 8f, barY2, cx2 + dp * 8f + barW, barY2 + barH2), barH2/2f, barH2/2f, p2Fill)
            val filled = barW * target * p2MacroProgress
            p2Fill.color = GREEN
            canvas.drawRoundRect(RectF(cx2 + dp * 8f, barY2, cx2 + dp * 8f + filled, barY2 + barH2), barH2/2f, barH2/2f, p2Fill)
        }
        y += 2f * (cellH + cellGap) + dp * 6f

        // Suggestion row
        if (p2MacroProgress > 0.5f) {
            val suggAlpha = ((p2MacroProgress - 0.5f) * 2f).coerceIn(0f, 1f)
            val sa = (suggAlpha * 255).toInt()
            p2Fill.color = Color.argb(min(sa, 255), 255, 248, 230)
            canvas.drawRoundRect(RectF(cardL + dp * 12f, y, cardR - dp * 12f, y + dp * 30f), dp * 10f, dp * 10f, p2Fill)
            p2TextL.color = Color.argb(sa, 139, 107, 10); p2TextL.textSize = dp * 9.5f; p2TextL.typeface = Typeface.DEFAULT
            canvas.drawText("💡  Add legumes or seeds to boost fiber by 4–6g", cardL + dp * 20f, y + dp * 19f, p2TextL)
            y += dp * 38f

            // Alt chips
            val altChips = listOf("🫘 Chickpeas", "🌻 Seeds", "🥦 Broccoli")
            var chipX = cardL + dp * 12f
            altChips.forEach { chip ->
                val cW = p2TextL.measureText(chip) + dp * 20f; val cH = dp * 22f
                p2Fill.color = Color.argb(sa, 225, 249, 240)
                canvas.drawRoundRect(RectF(chipX, y, chipX + cW, y + cH), cH/2f, cH/2f, p2Fill)
                p2Stroke.color = Color.argb(sa/2, 13, 191, 133); p2Stroke.strokeWidth = dp * 0.8f
                canvas.drawRoundRect(RectF(chipX, y, chipX + cW, y + cH), cH/2f, cH/2f, p2Stroke)
                p2TextL.color = Color.argb(sa, 10, 154, 106); p2TextL.textSize = dp * 9f; p2TextL.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(chip, chipX + dp * 10f, y + dp * 15f, p2TextL)
                chipX += cW + dp * 6f
            }
        }

        canvas.restore()
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t).pow(3f)

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 0 — ORB (unchanged from original)
    // ══════════════════════════════════════════════════════════════════════════

    private fun drawPremiumOrb(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f
        val orbRadius = min(w, h) * 0.25f
        val cy = h * 0.42f + orbFloatY
        val currentScale = orbTapScale * (1f + sin(breathPhase) * 0.018f)
        initParticlesIfNeeded(w, h)
        drawAmbientBg(canvas, w, h, cx, cy)
        drawParticles(canvas, w, h)
        drawOrbShadow(canvas, cx, cy + orbRadius * currentScale, orbRadius)
        drawOrbitBackAndRing(canvas, cx, cy, orbRadius)
        drawGlowRings(canvas, cx, cy, orbRadius, currentScale)
        drawOrbBody(canvas, cx, cy, orbRadius, currentScale)
        drawOrbFace(canvas, cx, cy, orbRadius, currentScale)
        drawOrbHighlights(canvas, cx, cy, orbRadius, currentScale)
        drawOrbitFront(canvas, cx, cy, orbRadius)
    }

    private fun initParticlesIfNeeded(w: Float, h: Float) {
        if (particlesInit) return
        particlesInit = true
        repeat(32) {
            particles.add(Particle(
                x = (Math.random() * w).toFloat(), y = (Math.random() * h).toFloat(),
                vx = ((Math.random() - 0.5) * 0.35).toFloat(), vy = (-Math.random() * 0.5 - 0.12).toFloat(),
                alpha = (Math.random() * 0.5 + 0.1).toFloat(), r = (Math.random() * 2.2 + 0.5).toFloat(),
                phase = (Math.random() * Math.PI * 2).toFloat()
            ))
        }
    }

    private fun drawAmbientBg(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val radialG1 = RadialGradient(cx, h * 0.1f, w * 0.7f,
            intArrayOf(Color.argb(28, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = radialG1; glowPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        val radialG2 = RadialGradient(cx, h * 0.85f, w * 0.6f,
            intArrayOf(Color.argb(20, 0, 152, 170), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = radialG2
        canvas.drawRect(0f, h * 0.5f, w, h, glowPaint)
        glowPaint.shader = null
    }

    private fun drawParticles(canvas: Canvas, w: Float, h: Float) {
        particles.forEach { p ->
            p.x += p.vx; p.y += p.vy; p.phase += 0.04f
            val a = p.alpha * (0.4f + 0.6f * ((sin(p.phase) + 1f) / 2f))
            particlePaint.color = Color.argb((a * 255).toInt().coerceIn(0, 255), 13, 191, 133)
            canvas.drawCircle(p.x, p.y, p.r, particlePaint)
            if (p.y < -8f) { p.y = h + 5f; p.x = (Math.random() * w).toFloat() }
            if (p.x < -5f || p.x > w + 5f) p.vx *= -1f
        }
    }

    private fun drawOrbShadow(canvas: Canvas, cx: Float, bottomY: Float, orbR: Float) {
        val shadowW = orbR * 1.6f; val shadowH = orbR * 0.22f
        val shadowGrad = RadialGradient(cx, bottomY + shadowH * 0.5f, shadowW,
            intArrayOf(Color.argb(70, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        shadowPaint.shader = shadowGrad
        shadowPaint.maskFilter = BlurMaskFilter(shadowH * 2f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawOval(RectF(cx - shadowW, bottomY, cx + shadowW, bottomY + shadowH * 2f), shadowPaint)
        shadowPaint.shader = null; shadowPaint.maskFilter = null
    }

    private fun getProjected(cx: Float, cy: Float, orbR: Float): List<Icon3D> {
        if (cachedProjectedTick == tick) return cachedProjected
        cachedProjectedTick = tick
        val orbitRadius = orbR * 1.78f
        val tiltX = Math.toRadians(62.0).toFloat(); val tiltZ = Math.toRadians(18.0).toFloat()
        val cosTX = cos(tiltX); val sinTX = sin(tiltX)
        val cosTZ = cos(tiltZ); val sinTZ = sin(tiltZ)
        cachedProjected = orbIcons.indices.map { i ->
            val theta = iconPhases[i] + orbitAngle
            val ox = orbitRadius * cos(theta); val oy = orbitRadius * sin(theta)
            val x1 = ox; val y1 = oy * cosTX; val z1 = oy * sinTX
            val x2 = x1 * cosTZ + y1 * sinTZ; val y2 = -x1 * sinTZ + y1 * cosTZ; val z2 = z1
            val wobble = sin(tick * 0.055f + iconFloatPhases[i]) * 3.5f
            Icon3D(cx + x2, cy + y2 + wobble, z2 / orbitRadius, i)
        }
        return cachedProjected
    }

    private fun drawOrbitBackAndRing(canvas: Canvas, cx: Float, cy: Float, orbR: Float) {
        val projected = getProjected(cx, cy, orbR)
        val iconBaseSize = orbR * 0.44f
        val orbitRadius = orbR * 1.78f
        val tiltX = Math.toRadians(62.0).toFloat(); val tiltZ = Math.toRadians(18.0).toFloat()
        projected.filter { it.depth <= 0f }.sortedBy { it.depth }
            .forEach { drawSingleIcon(canvas, it.screenX, it.screenY, it.depth, it.idx, iconBaseSize, orbR) }
        drawOrbitalRingLine(canvas, cx, cy, orbitRadius, tiltX, tiltZ)
    }

    private fun drawOrbitFront(canvas: Canvas, cx: Float, cy: Float, orbR: Float) {
        val projected = getProjected(cx, cy, orbR)
        val iconBaseSize = orbR * 0.44f
        projected.filter { it.depth > 0f }.sortedBy { it.depth }
            .forEach { drawSingleIcon(canvas, it.screenX, it.screenY, it.depth, it.idx, iconBaseSize, orbR) }
    }

    private fun drawSingleIcon(canvas: Canvas, sx: Float, sy: Float, depth: Float, idx: Int, baseSize: Float, orbR: Float) {
        val t = (depth + 1f) / 2f; val scale = 0.55f + t * 0.50f; val alpha = 0.35f + t * 0.65f
        val s = baseSize * scale
        if (depth > 0.1f) {
            val glowA = ((depth * 0.55f) * 255).toInt().coerceIn(0, 80)
            val grad = RadialGradient(sx, sy, s * 1.8f, intArrayOf(Color.argb(glowA, 13, 191, 133), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            glowPaint.shader = grad; glowPaint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, s * 1.8f, glowPaint); glowPaint.shader = null
        }
        iconBgPaint.color = Color.argb((alpha * 230).toInt().coerceIn(0, 255), 250, 247, 242)
        canvas.drawCircle(sx, sy, s, iconBgPaint)
        iconStrokePaint.color = Color.argb((alpha * 90).toInt().coerceIn(0, 255), 13, 191, 133)
        canvas.drawCircle(sx, sy, s, iconStrokePaint)
        iconTextPaint.textSize = s * 1.05f; iconTextPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawText(orbIcons[idx], sx, sy + s * 0.36f, iconTextPaint)
    }

    private fun drawOrbitalRingLine(canvas: Canvas, cx: Float, cy: Float, orbitRadius: Float, tiltX: Float, tiltZ: Float) {
        val cosTX = cos(tiltX); val sinTX = sin(tiltX); val cosTZ = cos(tiltZ); val sinTZ = sin(tiltZ)
        val steps = 120; val path = Path()
        for (step in 0..steps) {
            val theta = (step.toFloat() / steps) * 2f * PI.toFloat()
            val ox = orbitRadius * cos(theta); val oy = orbitRadius * sin(theta)
            val x1 = ox; val y1 = oy * cosTX; val z1 = oy * sinTX
            val x2 = x1 * cosTZ + y1 * sinTZ; val y2 = -x1 * sinTZ + y1 * cosTZ
            val sx = cx + x2; val sy = cy + y2
            if (step == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        ringPaint.style = Paint.Style.STROKE; ringPaint.strokeWidth = 3.5f
        ringPaint.color = Color.argb(40, 13, 191, 133); ringPaint.shader = null
        canvas.drawPath(path, ringPaint)
        val frontPath = Path(); var moved = false
        for (step in 0..steps) {
            val theta = (step.toFloat() / steps) * 2f * PI.toFloat()
            val oy = orbitRadius * sin(theta); val z1 = oy * sinTX
            val ox2 = orbitRadius * cos(theta); val y1 = oy * cosTX
            val x2b = ox2 * cosTZ + y1 * sinTZ; val y2b = -ox2 * sinTZ + y1 * cosTZ; val z2b = z1
            if (z2b > 0f) {
                val sx = cx + x2b; val sy = cy + y2b
                if (!moved) { frontPath.moveTo(sx, sy); moved = true } else frontPath.lineTo(sx, sy)
            } else if (moved) { moved = false }
        }
        ringPaint.color = Color.argb(140, 13, 191, 133); ringPaint.strokeWidth = 5.5f
        canvas.drawPath(frontPath, ringPaint)
    }

    private fun drawGlowRings(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val scaledR = orbR * scale
        val haloGrad = RadialGradient(cx, cy, scaledR * 1.7f,
            intArrayOf(Color.argb(55, 13, 191, 133), Color.argb(22, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0.4f, 0.7f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = haloGrad; glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, scaledR * 1.7f, glowPaint); glowPaint.shader = null
        canvas.save(); canvas.translate(cx, cy); canvas.scale(1f, 0.32f * scale)
        ringPaint.color = Color.argb(55, 13, 191, 133); ringPaint.strokeWidth = 2.8f; ringPaint.shader = null
        canvas.drawCircle(0f, 0f, scaledR * 1.18f, ringPaint); canvas.restore()
        canvas.save(); canvas.translate(cx, cy); canvas.scale(1f, 0.28f * scale); canvas.rotate(18f)
        ringPaint.color = Color.argb(35, 0, 152, 170); ringPaint.strokeWidth = 1.2f
        canvas.drawCircle(0f, 0f, scaledR * 1.1f, ringPaint); canvas.restore()
    }

    private fun drawOrbBody(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale
        val baseGrad = RadialGradient(cx - r * 0.18f, cy - r * 0.22f, r * 1.05f,
            intArrayOf(Color.parseColor("#1e8a60"), Color.parseColor("#0f5e46"), Color.parseColor("#0a4535"), Color.parseColor("#062e24")),
            floatArrayOf(0f, 0.35f, 0.65f, 1f), Shader.TileMode.CLAMP)
        orbPaint.shader = baseGrad; orbPaint.style = Paint.Style.FILL
        orbPaint.setShadowLayer(r * 0.5f, 0f, r * 0.12f, Color.argb(90, 0, 0, 0))
        canvas.drawCircle(cx, cy, r, orbPaint)
        orbPaint.clearShadowLayer(); orbPaint.shader = null
        val breathAlpha = (50 + sin(breathPhase) * 30).toInt()
        ringPaint.color = Color.argb(breathAlpha + 50, 13, 191, 133); ringPaint.strokeWidth = 2.2f; ringPaint.shader = null
        canvas.drawCircle(cx, cy, r, ringPaint)
    }

    private fun drawOrbFace(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale; val eyeSpacing = r * 0.28f; val eyeY = cy - r * 0.08f; val eyeR = r * 0.105f; val eyeGlowR = eyeR * 2.2f
        canvas.save(); canvas.scale(1f, eyeLeftScale, cx - eyeSpacing, eyeY)
        val leftGlow = RadialGradient(cx - eyeSpacing, eyeY, eyeGlowR, intArrayOf(Color.argb(80, 13, 191, 133), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = leftGlow; glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeGlowR, glowPaint); glowPaint.shader = null
        val leftEyeGrad = RadialGradient(cx - eyeSpacing - eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR, intArrayOf(Color.argb(240, 255, 255, 255), Color.argb(220, 13, 191, 133)), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        eyePaint.shader = leftEyeGrad; canvas.drawCircle(cx - eyeSpacing, eyeY, eyeR, eyePaint); eyePaint.shader = null; canvas.restore()
        canvas.save(); canvas.scale(1f, eyeRightScale, cx + eyeSpacing, eyeY)
        val rightGlow = RadialGradient(cx + eyeSpacing, eyeY, eyeGlowR, intArrayOf(Color.argb(80, 13, 191, 133), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = rightGlow; glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeGlowR, glowPaint); glowPaint.shader = null
        val rightEyeGrad = RadialGradient(cx + eyeSpacing - eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR, intArrayOf(Color.argb(240, 255, 255, 255), Color.argb(220, 13, 191, 133)), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        eyePaint.shader = rightEyeGrad; canvas.drawCircle(cx + eyeSpacing, eyeY, eyeR, eyePaint); eyePaint.shader = null; canvas.restore()
        val smileWidth = r * 0.44f; val smileTop = eyeY + r * 0.14f; val smileHeight = r * 0.20f
        val smileRect = RectF(cx - smileWidth, smileTop, cx + smileWidth, smileTop + smileHeight * 2f)
        smilePaint.color = Color.argb(220, 255, 255, 255); smilePaint.strokeWidth = r * 0.055f
        smilePaint.setShadowLayer(r * 0.08f, 0f, 0f, Color.argb(120, 13, 191, 133))
        canvas.drawArc(smileRect, 15f, 150f, false, smilePaint); smilePaint.clearShadowLayer()
    }

    private fun drawOrbHighlights(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale
        val hiX = cx - r * 0.22f; val hiY = cy - r * 0.26f
        val hiGrad = RadialGradient(hiX, hiY, r * 0.32f, intArrayOf(Color.argb(185, 255, 255, 255), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = hiGrad; highlightPaint.style = Paint.Style.FILL
        canvas.drawCircle(hiX, hiY, r * 0.32f, highlightPaint)
        val hi2X = cx + r * 0.3f; val hi2Y = cy - r * 0.34f
        val hi2Grad = RadialGradient(hi2X, hi2Y, r * 0.12f, intArrayOf(Color.argb(120, 255, 255, 255), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = hi2Grad; canvas.drawCircle(hi2X, hi2Y, r * 0.12f, highlightPaint)
        val riGrad = RadialGradient(cx, cy + r * 0.32f, r * 0.38f, intArrayOf(Color.argb(60, 13, 191, 133), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = riGrad; canvas.drawCircle(cx, cy + r * 0.32f, r * 0.38f, highlightPaint)
        highlightPaint.shader = null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 1 — Bars (unchanged)
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawBars(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val barCount = bars.size; val totalBarW = w * 0.72f; val barW = totalBarW / barCount * 0.62f
        val gap = totalBarW / barCount * 0.38f; val startX = (w - totalBarW) / 2f
        val maxH = h * 0.58f; val baseY = h * 0.82f
        bars.forEachIndexed { i, bar ->
            val x = startX + i * (barW + gap)
            val filledH = maxH * bar.target * barProgress
            val rect = RectF(x, baseY - maxH, x + barW, baseY)
            paintBarBg.color = Color.argb(30, 76, 175, 80)
            canvas.drawRoundRect(rect, 12f, 12f, paintBarBg)
            val fillRect = RectF(x, baseY - filledH, x + barW, baseY)
            paintBar.color = bar.color
            canvas.drawRoundRect(fillRect, 12f, 12f, paintBar)
            val pct = (bar.target * barProgress * 100).toInt()
            if (filledH > 24f) { paintValue.textSize = barW * 0.38f; canvas.drawText("$pct%", x + barW / 2f, baseY - filledH - 10f, paintValue) }
            paintLabel.textSize = barW * 0.28f; paintLabel.color = Color.argb(140, 255, 255, 255)
            canvas.drawText(bar.label, x + barW / 2f, baseY + 28f, paintLabel)
        }
    }
}