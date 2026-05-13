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
            invalidate()
        }

    // ── Shared ────────────────────────────────────────────────────────────────
    private var tick = 0f

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 0 — PREMIUM ORB
    // ══════════════════════════════════════════════════════════════════════════

    private var orbFloatY = 0f
    private var orbFloatPhase = 0f
    private var orbScale = 1f
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

    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val iconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    private val paintBar    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBarBg  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLabel  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF"); textAlign = Paint.Align.CENTER; textSize = 28f
    }
    private val paintValue  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8F5E9"); textAlign = Paint.Align.CENTER; textSize = 30f; isFakeBoldText = true
    }

    // ── PAGE 2: Chat ──────────────────────────────────────────────────────────
    private data class Bubble(val text: String, val isUser: Boolean)
    private val bubbles = listOf(
        Bubble("What should I eat today?", true),
        Bubble("Try high-protein breakfast — eggs + avocado 🥑", false),
        Bubble("Track lunch: grilled chicken", true),
        Bubble("Added! 340 kcal · 38g protein ✓", false),
    )
    private var chatStep = 0
    private val paintBubbleUser = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B5E20") }
    private val paintBubbleBot  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A2E1A") }
    private val paintBubbleText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f }
    private val bubbleRect = RectF()

    // 3D projection cache
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
                2 -> if (tick % 42 == 0f && chatStep < bubbles.size) chatStep++
            }
            invalidate()
            postDelayed(this, 16L)
        }
    }

    private fun tickOrb() {
        breathPhase += 0.018f
        orbFloatPhase += 0.012f
        orbFloatY = sin(orbFloatPhase) * 10f
        orbitAngle += 0.008f

        if (orbTapDecay > 0f) {
            orbTapDecay = max(0f, orbTapDecay - 0.06f)
            orbTapScale = 1f + orbTapDecay * 0.12f
        }

        blinkTimer += 1f
        if (blinkTimer >= blinkInterval) {
            blinkTimer = 0f
            blinkInterval = 120f + (Math.random() * 220f).toFloat()
            isWink = Math.random() < 0.28
            blinkPhase = 0f
        }
        if (blinkPhase < 1f) {
            blinkPhase = min(1f, blinkPhase + 0.14f)
            val blink = sin(blinkPhase * PI.toFloat())
            eyeLeftScale = 1f - blink * 0.92f
            eyeRightScale = if (isWink) 1f else 1f - blink * 0.92f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (pageIndex == 0 && event.action == MotionEvent.ACTION_DOWN) {
            val cx = width / 2f
            val cy = height * 0.42f + orbFloatY
            val dx = event.x - cx
            val dy = event.y - cy
            val orbR = min(width, height) * 0.25f
            if (dx * dx + dy * dy < orbR * orbR * 1.4f) {
                orbTapDecay = 1f
                isWink = true
                blinkPhase = 0f
                blinkTimer = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (pageIndex) {
            0 -> drawPremiumOrb(canvas)
            1 -> drawBars(canvas)
            2 -> drawChat(canvas)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // drawPremiumOrb — Page 0
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawPremiumOrb(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
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
                x = (Math.random() * w).toFloat(),
                y = (Math.random() * h).toFloat(),
                vx = ((Math.random() - 0.5) * 0.35).toFloat(),
                vy = (-Math.random() * 0.5 - 0.12).toFloat(),
                alpha = (Math.random() * 0.5 + 0.1).toFloat(),
                r = (Math.random() * 2.2 + 0.5).toFloat(),
                phase = (Math.random() * Math.PI * 2).toFloat()
            ))
        }
    }

    private fun drawAmbientBg(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val radialG1 = RadialGradient(cx, h * 0.1f, w * 0.7f,
            intArrayOf(Color.argb(28, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = radialG1
        glowPaint.style = Paint.Style.FILL
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
            p.x += p.vx
            p.y += p.vy
            p.phase += 0.04f
            val a = p.alpha * (0.4f + 0.6f * ((sin(p.phase) + 1f) / 2f))
            particlePaint.color = Color.argb((a * 255).toInt().coerceIn(0, 255), 13, 191, 133)
            canvas.drawCircle(p.x, p.y, p.r, particlePaint)
            if (p.y < -8f) { p.y = h + 5f; p.x = (Math.random() * w).toFloat() }
            if (p.x < -5f || p.x > w + 5f) p.vx *= -1f
        }
    }

    private fun drawOrbShadow(canvas: Canvas, cx: Float, bottomY: Float, orbR: Float) {
        val shadowW = orbR * 1.6f
        val shadowH = orbR * 0.22f
        val shadowGrad = RadialGradient(cx, bottomY + shadowH * 0.5f, shadowW,
            intArrayOf(Color.argb(70, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        shadowPaint.shader = shadowGrad
        shadowPaint.maskFilter = BlurMaskFilter(shadowH * 2f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawOval(RectF(cx - shadowW, bottomY, cx + shadowW, bottomY + shadowH * 2f), shadowPaint)
        shadowPaint.shader = null
        shadowPaint.maskFilter = null
    }

    private fun getProjected(cx: Float, cy: Float, orbR: Float): List<Icon3D> {
        if (cachedProjectedTick == tick) return cachedProjected
        cachedProjectedTick = tick

        val orbitRadius = orbR * 1.78f
        val tiltX = Math.toRadians(62.0).toFloat()
        val tiltZ = Math.toRadians(18.0).toFloat()
        val cosTX = cos(tiltX); val sinTX = sin(tiltX)
        val cosTZ = cos(tiltZ); val sinTZ = sin(tiltZ)

        cachedProjected = orbIcons.indices.map { i ->
            val theta = iconPhases[i] + orbitAngle
            val ox = orbitRadius * cos(theta)
            val oy = orbitRadius * sin(theta)

            val x1 = ox
            val y1 = oy * cosTX
            val z1 = oy * sinTX

            val x2 = x1 * cosTZ + y1 * sinTZ
            val y2 = -x1 * sinTZ + y1 * cosTZ
            val z2 = z1

            val wobble = sin(tick * 0.055f + iconFloatPhases[i]) * 3.5f
            val depth = z2 / orbitRadius

            Icon3D(cx + x2, cy + y2 + wobble, depth, i)
        }
        return cachedProjected
    }

    private fun drawOrbitBackAndRing(canvas: Canvas, cx: Float, cy: Float, orbR: Float) {
        val projected = getProjected(cx, cy, orbR)
        val iconBaseSize = orbR * 0.44f
        val orbitRadius = orbR * 1.78f
        val tiltX = Math.toRadians(62.0).toFloat()
        val tiltZ = Math.toRadians(18.0).toFloat()

        projected.filter { it.depth <= 0f }
            .sortedBy { it.depth }
            .forEach { drawSingleIcon(canvas, it.screenX, it.screenY, it.depth, it.idx, iconBaseSize, orbR) }

        drawOrbitalRingLine(canvas, cx, cy, orbitRadius, tiltX, tiltZ)
    }

    private fun drawOrbitFront(canvas: Canvas, cx: Float, cy: Float, orbR: Float) {
        val projected = getProjected(cx, cy, orbR)
        val iconBaseSize = orbR * 0.44f

        projected.filter { it.depth > 0f }
            .sortedBy { it.depth }
            .forEach { drawSingleIcon(canvas, it.screenX, it.screenY, it.depth, it.idx, iconBaseSize, orbR) }
    }

    private fun drawSingleIcon(
        canvas: Canvas,
        sx: Float, sy: Float,
        depth: Float,
        idx: Int,
        baseSize: Float,
        orbR: Float
    ) {
        val t = (depth + 1f) / 2f
        val scale = 0.55f + t * 0.50f
        val alpha = 0.35f + t * 0.65f
        val s = baseSize * scale

        if (depth > 0.1f) {
            val glowA = ((depth * 0.55f) * 255).toInt().coerceIn(0, 80)
            val grad = RadialGradient(sx, sy, s * 1.8f,
                intArrayOf(Color.argb(glowA, 13, 191, 133), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            glowPaint.shader = grad
            glowPaint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, s * 1.8f, glowPaint)
            glowPaint.shader = null
        }

        iconBgPaint.color = Color.argb((alpha * 230).toInt().coerceIn(0, 255), 250, 247, 242)
        canvas.drawCircle(sx, sy, s, iconBgPaint)

        iconStrokePaint.color = Color.argb((alpha * 90).toInt().coerceIn(0, 255), 13, 191, 133)
        canvas.drawCircle(sx, sy, s, iconStrokePaint)

        iconTextPaint.textSize = s * 1.05f
        iconTextPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawText(orbIcons[idx], sx, sy + s * 0.36f, iconTextPaint)
    }

    private fun drawOrbitalRingLine(
        canvas: Canvas, cx: Float, cy: Float,
        orbitRadius: Float,
        tiltX: Float, tiltZ: Float
    ) {
        val cosTX = cos(tiltX); val sinTX = sin(tiltX)
        val cosTZ = cos(tiltZ); val sinTZ = sin(tiltZ)
        val steps = 120
        val path = Path()

        for (step in 0..steps) {
            val theta = (step.toFloat() / steps) * 2f * PI.toFloat()
            val ox = orbitRadius * cos(theta)
            val oy = orbitRadius * sin(theta)

            val x1 = ox
            val y1 = oy * cosTX
            val z1 = oy * sinTX

            val x2 = x1 * cosTZ + y1 * sinTZ
            val y2 = -x1 * sinTZ + y1 * cosTZ
            val z2 = z1

            val sx = cx + x2
            val sy = cy + y2

            if (step == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }



        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = 3.5f
        ringPaint.color = Color.argb(40, 13, 191, 133)
        ringPaint.shader = null
        canvas.drawPath(path, ringPaint)

        val frontPath = Path()
        var moved = false
        for (step in 0..steps) {
            val theta = (step.toFloat() / steps) * 2f * PI.toFloat()
            val oy = orbitRadius * sin(theta)
            val z1 = oy * sinTX
            val ox2 = orbitRadius * cos(theta)
            val y1 = oy * cosTX
            val x2b = ox2 * cosTZ + y1 * sinTZ
            val y2b = -ox2 * sinTZ + y1 * cosTZ
            val z2b = z1
            if (z2b > 0f) {
                val sx = cx + x2b
                val sy = cy + y2b
                if (!moved) { frontPath.moveTo(sx, sy); moved = true }
                else frontPath.lineTo(sx, sy)
            } else if (moved) {
                moved = false
            }
        }
        ringPaint.color = Color.argb(140, 13, 191, 133)
        ringPaint.strokeWidth = 5.5f
        canvas.drawPath(frontPath, ringPaint)
    }

    private fun drawGlowRings(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val scaledR = orbR * scale

        val haloGrad = RadialGradient(cx, cy, scaledR * 1.7f,
            intArrayOf(
                Color.argb(55, 13, 191, 133),
                Color.argb(22, 13, 191, 133),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.4f, 0.7f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = haloGrad
        glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, scaledR * 1.7f, glowPaint)
        glowPaint.shader = null

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, 0.32f * scale)
        ringPaint.color = Color.argb(55, 13, 191, 133)
        ringPaint.strokeWidth = 2.8f
        ringPaint.shader = null
        canvas.drawCircle(0f, 0f, scaledR * 1.18f, ringPaint)
        canvas.restore()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, 0.28f * scale)
        canvas.rotate(18f)
        ringPaint.color = Color.argb(35, 0, 152, 170)
        ringPaint.strokeWidth = 1.2f
        canvas.drawCircle(0f, 0f, scaledR * 1.1f, ringPaint)
        canvas.restore()
    }

    private fun drawOrbBody(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale

        val baseGrad = RadialGradient(
            cx - r * 0.18f, cy - r * 0.22f, r * 1.05f,
            intArrayOf(
                Color.parseColor("#1e8a60"),
                Color.parseColor("#0f5e46"),
                Color.parseColor("#0a4535"),
                Color.parseColor("#062e24")
            ),
            floatArrayOf(0f, 0.35f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        orbPaint.shader = baseGrad
        orbPaint.style = Paint.Style.FILL
        orbPaint.setShadowLayer(r * 0.5f, 0f, r * 0.12f, Color.argb(90, 0, 0, 0))
        canvas.drawCircle(cx, cy, r, orbPaint)
        orbPaint.clearShadowLayer()
        orbPaint.shader = null

        val breathAlpha = (50 + sin(breathPhase) * 30).toInt()
        ringPaint.color = Color.argb(breathAlpha + 50, 13, 191, 133)
        ringPaint.strokeWidth = 2.2f
        ringPaint.shader = null
        canvas.drawCircle(cx, cy, r, ringPaint)
    }

    private fun drawOrbFace(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale
        val eyeSpacing = r * 0.28f
        val eyeY = cy - r * 0.08f
        val eyeR = r * 0.105f
        val eyeGlowR = eyeR * 2.2f

        // Left eye
        canvas.save()
        canvas.scale(1f, eyeLeftScale, cx - eyeSpacing, eyeY)
        val leftGlow = RadialGradient(cx - eyeSpacing, eyeY, eyeGlowR,
            intArrayOf(Color.argb(80, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = leftGlow
        glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeGlowR, glowPaint)
        glowPaint.shader = null

        val leftEyeGrad = RadialGradient(
            cx - eyeSpacing - eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR,
            intArrayOf(Color.argb(240, 255, 255, 255), Color.argb(220, 13, 191, 133)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        eyePaint.shader = leftEyeGrad
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeR, eyePaint)
        eyePaint.shader = null
        canvas.restore()

        // Right eye
        canvas.save()
        canvas.scale(1f, eyeRightScale, cx + eyeSpacing, eyeY)
        val rightGlow = RadialGradient(cx + eyeSpacing, eyeY, eyeGlowR,
            intArrayOf(Color.argb(80, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = rightGlow
        glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeGlowR, glowPaint)
        glowPaint.shader = null

        val rightEyeGrad = RadialGradient(
            cx + eyeSpacing - eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR,
            intArrayOf(Color.argb(240, 255, 255, 255), Color.argb(220, 13, 191, 133)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        eyePaint.shader = rightEyeGrad
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeR, eyePaint)
        eyePaint.shader = null
        canvas.restore()

        // Smile
        val smileWidth = r * 0.44f
        val smileTop = eyeY + r * 0.14f
        val smileHeight = r * 0.20f
        val smileRect = RectF(cx - smileWidth, smileTop, cx + smileWidth, smileTop + smileHeight * 2f)
        smilePaint.color = Color.argb(220, 255, 255, 255)
        smilePaint.strokeWidth = r * 0.055f
        smilePaint.setShadowLayer(r * 0.08f, 0f, 0f, Color.argb(120, 13, 191, 133))
        canvas.drawArc(smileRect, 15f, 150f, false, smilePaint)
        smilePaint.clearShadowLayer()
    }

    private fun drawOrbHighlights(canvas: Canvas, cx: Float, cy: Float, orbR: Float, scale: Float) {
        val r = orbR * scale

        val hiX = cx - r * 0.22f
        val hiY = cy - r * 0.26f
        val hiGrad = RadialGradient(hiX, hiY, r * 0.32f,
            intArrayOf(Color.argb(185, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = hiGrad
        highlightPaint.style = Paint.Style.FILL
        canvas.drawCircle(hiX, hiY, r * 0.32f, highlightPaint)

        val hi2X = cx + r * 0.3f
        val hi2Y = cy - r * 0.34f
        val hi2Grad = RadialGradient(hi2X, hi2Y, r * 0.12f,
            intArrayOf(Color.argb(120, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = hi2Grad
        canvas.drawCircle(hi2X, hi2Y, r * 0.12f, highlightPaint)

        val riX = cx
        val riY = cy + r * 0.32f
        val riGrad = RadialGradient(riX, riY, r * 0.38f,
            intArrayOf(Color.argb(60, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        highlightPaint.shader = riGrad
        canvas.drawCircle(riX, riY, r * 0.38f, highlightPaint)

        highlightPaint.shader = null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 1 — Macro Bars
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawBars(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val barCount = bars.size
        val totalBarW = w * 0.72f
        val barW = totalBarW / barCount * 0.62f
        val gap = totalBarW / barCount * 0.38f
        val startX = (w - totalBarW) / 2f
        val maxH = h * 0.58f
        val baseY = h * 0.82f

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
            if (filledH > 24f) {
                paintValue.textSize = barW * 0.38f
                canvas.drawText("$pct%", x + barW / 2f, baseY - filledH - 10f, paintValue)
            }
            paintLabel.textSize = barW * 0.28f
            paintLabel.color = Color.argb(140, 255, 255, 255)
            canvas.drawText(bar.label, x + barW / 2f, baseY + 28f, paintLabel)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAGE 2 — Chat Bubbles
    // ══════════════════════════════════════════════════════════════════════════
    private fun drawChat(canvas: Canvas) {
        val w = width.toFloat()
        val padding = w * 0.05f
        val maxBubbleW = w * 0.70f
        val bubbleH = height * 0.16f
        val verticalStep = height * 0.20f
        val startY = height * 0.08f

        paintBubbleText.textSize = w * 0.038f

        val visible = min(chatStep, bubbles.size)
        for (i in 0 until visible) {
            val b = bubbles[i]
            val y = startY + i * verticalStep
            val bubbleW = maxBubbleW
            val alpha = if (i == visible - 1) min(1f, (tick % 42f) / 14f) else 1f
            val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)

            if (b.isUser) {
                paintBubbleUser.alpha = alphaInt
                bubbleRect.set(w - padding - bubbleW, y, w - padding, y + bubbleH)
                canvas.drawRoundRect(bubbleRect, 18f, 18f, paintBubbleUser)
                paintBubbleText.color = Color.argb(alphaInt, 232, 245, 233)
                paintBubbleText.textAlign = Paint.Align.RIGHT
                drawWrappedText(canvas, b.text, w - padding - 16f, y + bubbleH * 0.38f, paintBubbleText, bubbleW - 24f)
            } else {
                paintBubbleBot.alpha = alphaInt
                bubbleRect.set(padding, y, padding + bubbleW, y + bubbleH)
                canvas.drawRoundRect(bubbleRect, 18f, 18f, paintBubbleBot)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 1.5f
                    color = Color.argb(alphaInt / 3, 76, 175, 80)
                }
                canvas.drawRoundRect(bubbleRect, 18f, 18f, borderPaint)
                paintBubbleText.color = Color.argb(alphaInt, 200, 230, 201)
                paintBubbleText.textAlign = Paint.Align.LEFT
                drawWrappedText(canvas, b.text, padding + 16f, y + bubbleH * 0.38f, paintBubbleText, bubbleW - 24f)
            }
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, maxWidth: Float) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = ""
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) <= maxWidth) line = test
            else { if (line.isNotEmpty()) lines.add(line); line = word }
        }
        if (line.isNotEmpty()) lines.add(line)
        val lineH = paint.textSize * 1.35f
        lines.forEachIndexed { i, l -> canvas.drawText(l, x, y + i * lineH, paint) }
    }
}