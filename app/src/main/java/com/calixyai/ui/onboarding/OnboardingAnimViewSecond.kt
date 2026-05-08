package com.calixyai.ui.onboarding

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.*


class OnboardingAnimViewSecond @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Constants ─────────────────────────────────────────────────────────────

    private val COLOR_GREEN_1 = Color.parseColor("#0DBF85")
    private val COLOR_GREEN_2 = Color.parseColor("#0AA873")
    private val COLOR_TEAL    = Color.parseColor("#0098AA")
    private val COLOR_GREEN_3 = Color.parseColor("#07A060")
    private val COLOR_GREEN_DARK = Color.parseColor("#065C42")

    private data class TubeData(
        val label: String,
        val targetFill: Float,  // 0..1
        val colorTop: Int,
        val colorBot: Int,
        val glowColor: Int
    )

    private val tubes = listOf(
        TubeData("Protein", 0.78f, Color.parseColor("#0DBF85"), Color.parseColor("#07824A"), Color.parseColor("#0DBF85")),
        TubeData("Carbs",   0.52f, Color.parseColor("#00C8A0"), Color.parseColor("#00876B"), Color.parseColor("#00C8A0")),
        TubeData("Fats",    0.35f, Color.parseColor("#0DBF85"), Color.parseColor("#068050"), Color.parseColor("#0DBF85")),
        TubeData("Fiber",   0.61f, Color.parseColor("#00B496"), Color.parseColor("#007B60"), Color.parseColor("#00B496"))
    )

    // ── Animation state ───────────────────────────────────────────────────────

    private val fillProgress = FloatArray(4) { 0f }   // 0..1 each tube
    private var tick = 0f
    private var orbitAngle = 0f
    private var platformGlowAlpha = 0f

    // Particles per tube
    private data class Particle(var relX: Float, var relY: Float, var alpha: Float, var phase: Float, var radius: Float)
    private val particles: Array<MutableList<Particle>> = Array(4) { mutableListOf() }

    // ── Animators ─────────────────────────────────────────────────────────────

    private lateinit var fillAnimator: ValueAnimator
    private val frameRunnable = object : Runnable {
        override fun run() {
            tick += 1f
            orbitAngle += 0.006f
            platformGlowAlpha = 0.4f + 0.2f * sin(tick * 0.04f)
            // Age particles
            particles.forEachIndexed { ti, list ->
                list.forEach { p ->
                    p.relY -= 0.006f
                    p.phase += 0.05f
                    p.alpha = (0.8f * sin(p.phase * PI.toFloat() / 3f)).coerceIn(0f, 0.9f)
                    if (p.relY < 0f) {
                        p.relY = fillProgress[ti] * 0.95f
                        p.relX = 0.2f + Math.random().toFloat() * 0.6f
                        p.phase = 0f
                    }
                }
            }
            invalidate()
            postDelayed(this, 16L)
        }
    }

    // ── Paints ────────────────────────────────────────────────────────────────

    private val glowPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tubeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }
    private val reflPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val pctPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val iconBgPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconTxtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val floatIcons = listOf("🍗", "🌿", "💧", "❤️")
    // icon float phases for independent animation
    private val iconPhases = floatArrayOf(0f, 1.2f, 2.4f, 3.6f)

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        // Seed particles
        repeat(4) { ti ->
            repeat(3) {
                particles[ti].add(
                    Particle(
                        relX   = 0.2f + Math.random().toFloat() * 0.6f,
                        relY   = Math.random().toFloat(),
                        alpha  = 0f,
                        phase  = Math.random().toFloat() * 6f,
                        radius = (2f + Math.random().toFloat() * 2.5f)
                    )
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Start fill animation after short delay
        fillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            startDelay = 300
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                tubes.forEachIndexed { i, tube ->
                    fillProgress[i] = p * tube.targetFill
                }
                invalidate()
            }
            start()
        }
        post(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (::fillAnimator.isInitialized) fillAnimator.cancel()
        removeCallbacks(frameRunnable)
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        drawAmbientBackground(canvas, w, h)
        drawOrbitalRings(canvas, w, h)
        drawPlatformGlow(canvas, w, h)
        drawTubes(canvas, w, h)
        drawFloatingIcons(canvas, w, h)
    }

    // ── Ambient background glows ──────────────────────────────────────────────

    private fun drawAmbientBackground(canvas: Canvas, w: Float, h: Float) {
        // Top-right glow
        val r1 = RadialGradient(
            w * 0.85f, h * 0.05f, w * 0.55f,
            intArrayOf(Color.argb(22, 13, 191, 133), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        glowPaint.shader = r1; glowPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, glowPaint)

        // Bottom-left glow
        val r2 = RadialGradient(
            w * 0.1f, h * 0.88f, w * 0.45f,
            intArrayOf(Color.argb(15, 0, 152, 170), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        glowPaint.shader = r2
        canvas.drawRect(0f, h * 0.5f, w, h, glowPaint)
        glowPaint.shader = null
    }

    // ── Orbital rings ─────────────────────────────────────────────────────────

    private fun drawOrbitalRings(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h * 0.50f

        // Ring 1 — tilt 62° X, 18° Z, radius w*0.42
        drawTiltedRing(canvas, cx, cy, w * 0.42f, 62.0, 18.0,
            outerAlpha = 35, innerAlpha = 110, strokeOuter = 2.5f, strokeInner = 4.5f,
            color = COLOR_GREEN_1)

        // Ring 2 — reverse tilt, slightly larger
        drawTiltedRing(canvas, cx, cy, w * 0.50f, 58.0, -14.0,
            outerAlpha = 18, innerAlpha = 55, strokeOuter = 1.5f, strokeInner = 2.5f,
            color = COLOR_TEAL)
    }

    private fun drawTiltedRing(
        canvas: Canvas, cx: Float, cy: Float, orbitR: Float,
        tiltXDeg: Double, tiltZDeg: Double,
        outerAlpha: Int, innerAlpha: Int,
        strokeOuter: Float, strokeInner: Float,
        color: Int
    ) {
        val tiltX = Math.toRadians(tiltXDeg).toFloat()
        val tiltZ = Math.toRadians(tiltZDeg + orbitAngle * 57.3).toFloat()
        val cosTX = cos(tiltX); val sinTX = sin(tiltX)
        val cosTZ = cos(tiltZ); val sinTZ = sin(tiltZ)
        val steps = 120

        val path = Path()
        val frontPath = Path()
        var frontMoved = false

        for (step in 0..steps) {
            val theta = (step.toFloat() / steps) * 2f * PI.toFloat()
            val ox = orbitR * cos(theta)
            val oy = orbitR * sin(theta)
            val x1 = ox; val y1 = oy * cosTX; val z1 = oy * sinTX
            val x2 = x1 * cosTZ + y1 * sinTZ
            val y2 = -x1 * sinTZ + y1 * cosTZ
            val z2 = z1
            val sx = cx + x2; val sy = cy + y2

            if (step == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)

            if (z2 > 0f) {
                if (!frontMoved) { frontPath.moveTo(sx, sy); frontMoved = true }
                else frontPath.lineTo(sx, sy)
            } else if (frontMoved) { frontMoved = false }
        }

        ringPaint.strokeWidth = strokeOuter
        ringPaint.color = Color.argb(outerAlpha, Color.red(color), Color.green(color), Color.blue(color))
        ringPaint.shader = null
        canvas.drawPath(path, ringPaint)

        ringPaint.strokeWidth = strokeInner
        ringPaint.color = Color.argb(innerAlpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawPath(frontPath, ringPaint)
    }

    // ── Platform glow ─────────────────────────────────────────────────────────

    private fun drawPlatformGlow(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        // FIX: match tubes baseY (62%)
        val baseY = h * 0.62f
        val grad = RadialGradient(
            cx, baseY, w * 0.30f,
            intArrayOf(
                Color.argb((platformGlowAlpha * 120).toInt(), 13, 191, 133),
                Color.argb((platformGlowAlpha * 50).toInt(), 13, 191, 133),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        glowPaint.shader = grad
        glowPaint.style = Paint.Style.FILL
        canvas.save()
        canvas.scale(1f, 0.25f, cx, baseY)
        canvas.drawCircle(cx, baseY, w * 0.30f, glowPaint)
        canvas.restore()
        glowPaint.shader = null
    }

    // ── Tubes ─────────────────────────────────────────────────────────────────

    private fun drawTubes(canvas: Canvas, w: Float, h: Float) {
        val tubeCount = 4
        val dp = resources.displayMetrics.density

        // Narrower tubes with equal gaps so labels fit without overlap
        val tubeW = w * 0.115f
        val gap = w * 0.045f
        val totalW = tubeCount * tubeW + (tubeCount - 1) * gap
        val startX = (w - totalW) / 2f

        // Tubes end at 62% — bottom 28% is reserved for labels
        val baseY = h * 0.62f
        val maxH = h * 0.40f

        // Fixed label zone: label name on first line, pct on second, well separated
        val labelZoneY = baseY + dp * 14f   // tube name
        val pctZoneY   = labelZoneY + dp * 17f  // percentage — clear gap below name

        // Heights vary slightly per tube for natural feel
        val tubeHeights = floatArrayOf(maxH, maxH * 0.92f, maxH * 0.84f, maxH * 0.96f)

        tubes.forEachIndexed { i, tube ->
            // FIX: floatOffset only affects the tube visual, NOT the labels
            val floatOffset = sin(tick * 0.025f + i * 0.8f) * dp * 4f
            val x = startX + i * (tubeW + gap)
            val tH = tubeHeights[i]
            val top = baseY - tH + floatOffset
            val tubeRect = RectF(x, top, x + tubeW, baseY + floatOffset)
            val cornerR = tubeW * 0.45f

            // Outer ambient glow
            val outerGlow = RadialGradient(
                x + tubeW / 2f, tubeRect.centerY(), tubeW * 1.2f,
                intArrayOf(Color.argb(30, 13, 191, 133), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
            glowPaint.shader = outerGlow
            glowPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                RectF(x - tubeW * 0.3f, tubeRect.top, x + tubeW + tubeW * 0.3f, tubeRect.bottom),
                cornerR, cornerR, glowPaint
            )
            glowPaint.shader = null

            // Tube background (glass body)
            val glassBg = LinearGradient(
                x, 0f, x + tubeW, 0f,
                intArrayOf(
                    Color.argb(100, 255, 255, 255),
                    Color.argb(55, 255, 255, 255),
                    Color.argb(30, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            tubeBgPaint.shader = glassBg
            canvas.drawRoundRect(tubeRect, cornerR, cornerR, tubeBgPaint)
            tubeBgPaint.shader = null

            // Tube inner track
            val trackRect = RectF(
                x + tubeW * 0.12f,
                top + tubeW * 0.12f,
                x + tubeW - tubeW * 0.12f,
                baseY + floatOffset - tubeW * 0.06f
            )
            val trackCorner = (tubeW - tubeW * 0.24f) * 0.45f
            tubeBgPaint.shader = null
            tubeBgPaint.color = Color.argb(20, 0, 0, 0)
            canvas.drawRoundRect(trackRect, trackCorner, trackCorner, tubeBgPaint)

            // Liquid fill
            val fillH = (trackRect.height() * fillProgress[i]).coerceAtLeast(0f)
            if (fillH > 2f) {
                val fillTop = trackRect.bottom - fillH
                val fillRect = RectF(trackRect.left, fillTop, trackRect.right, trackRect.bottom)

                val fillGrad = LinearGradient(
                    0f, fillTop, 0f, trackRect.bottom,
                    intArrayOf(
                        Color.argb(230, Color.red(tube.colorTop), Color.green(tube.colorTop), Color.blue(tube.colorTop)),
                        Color.argb(255, Color.red(tube.colorBot), Color.green(tube.colorBot), Color.blue(tube.colorBot))
                    ),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                )
                fillPaint.shader = fillGrad
                canvas.drawRoundRect(fillRect, trackCorner, trackCorner, fillPaint)
                fillPaint.shader = null

                // Liquid glow (top edge)
                val topGlowGrad = RadialGradient(
                    fillRect.centerX(), fillTop, tubeW * 0.6f,
                    intArrayOf(Color.argb(90, Color.red(tube.glowColor), Color.green(tube.glowColor), Color.blue(tube.glowColor)), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                )
                glowPaint.shader = topGlowGrad
                glowPaint.style = Paint.Style.FILL
                canvas.drawRect(fillRect.left, fillTop - tubeW * 0.2f, fillRect.right, fillTop + tubeW * 0.3f, glowPaint)
                glowPaint.shader = null

                // Wave on liquid surface
                drawLiquidWave(canvas, fillRect.left, fillRect.right, fillTop, tube.colorTop)

                // Particles inside liquid
                drawTubeParticles(canvas, i, trackRect, fillProgress[i])
            }

            // Glass stroke (border)
            val glassStroke = LinearGradient(
                x, 0f, x + tubeW, 0f,
                intArrayOf(
                    Color.argb(200, 255, 255, 255),
                    Color.argb(80, 255, 255, 255),
                    Color.argb(40, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            glassPaint.shader = glassStroke
            canvas.drawRoundRect(tubeRect, cornerR, cornerR, glassPaint)
            glassPaint.shader = null

            // Left reflection highlight
            val reflW = tubeW * 0.18f
            val reflGrad = LinearGradient(
                x + tubeW * 0.12f, 0f,
                x + tubeW * 0.12f + reflW, 0f,
                intArrayOf(Color.argb(130, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
            reflPaint.shader = reflGrad
            val reflTop = top + tubeW * 0.15f
            val reflBot = top + tH * 0.55f + floatOffset
            canvas.drawRoundRect(
                RectF(x + tubeW * 0.14f, reflTop, x + tubeW * 0.14f + reflW, reflBot),
                reflW / 2f, reflW / 2f, reflPaint
            )
            reflPaint.shader = null

            // Labels drawn at fixed y positions (no floatOffset) — never overlap
            val tubeCenterX = x + tubeW / 2f

            labelPaint.textSize = 12f * dp
            labelPaint.color = Color.argb(160, 20, 50, 30)
            labelPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(tube.label, tubeCenterX, labelZoneY, labelPaint)

            pctPaint.textSize = 14f * dp
            pctPaint.color = COLOR_GREEN_1
            pctPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${(fillProgress[i] * 100).toInt()}%", tubeCenterX, pctZoneY, pctPaint)
        }
    }

    // ── Wave on liquid top ────────────────────────────────────────────────────

    private fun drawLiquidWave(canvas: Canvas, left: Float, right: Float, y: Float, color: Int) {
        val path = Path()
        val segments = 8
        val segW = (right - left) / segments
        path.moveTo(left, y)
        for (s in 0 until segments) {
            val x0 = left + s * segW
            val x1 = left + (s + 1) * segW
            val waveH = sin(tick * 0.07f + s * 0.9f) * 2.5f
            path.quadTo(x0 + segW / 2f, y + waveH, x1, y)
        }
        wavePaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawPath(path, wavePaint)
    }

    // ── Particles ─────────────────────────────────────────────────────────────

    private fun drawTubeParticles(canvas: Canvas, tubeIdx: Int, trackRect: RectF, fill: Float) {
        val tW = trackRect.width()
        val tH = trackRect.height()
        particles[tubeIdx].forEach { p ->
            if (p.relY > 1f - fill) return@forEach
            val px = trackRect.left + p.relX * tW
            val py = trackRect.bottom - (p.relY / fill) * fill * tH
            particlePaint.color = Color.argb((p.alpha * 200).toInt().coerceIn(0, 200), 255, 255, 255)
            canvas.drawCircle(px, py, p.radius, particlePaint)
        }
    }

    // ── Floating icons ────────────────────────────────────────────────────────

    private fun drawFloatingIcons(canvas: Canvas, w: Float, h: Float) {
        val dp = resources.displayMetrics.density
        // Smaller icons — was dp*21, now dp*14
        val iconR = dp * 14f

        // Keep only the two top-corner icons; skip bottom ones that overlap tubes/labels
        val positions = listOf(
            Pair(w * 0.08f, h * 0.07f),   // top-left
            Pair(w * 0.88f, h * 0.07f)    // top-right
        )
        val iconsSubset = listOf(floatIcons[0], floatIcons[1])
        val phasesSubset = listOf(iconPhases[0], iconPhases[1])

        positions.forEachIndexed { i, (bx, by) ->
            val floatY = sin(tick * 0.022f + phasesSubset[i]) * dp * 5f
            val cx = bx; val cy = by + floatY

            // Small subtle glow only
            val gGrad = RadialGradient(
                cx, cy, iconR * 1.6f,
                intArrayOf(Color.argb(30, 13, 191, 133), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
            glowPaint.shader = gGrad
            glowPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, iconR * 1.6f, glowPaint)
            glowPaint.shader = null

            // Icon background circle
            val bgGrad = RadialGradient(
                cx - iconR * 0.25f, cy - iconR * 0.25f, iconR,
                intArrayOf(Color.argb(220, 255, 255, 255), Color.argb(140, 245, 250, 248)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
            iconBgPaint.shader = bgGrad
            canvas.drawCircle(cx, cy, iconR, iconBgPaint)

            // Circle border
            glassPaint.color = Color.argb(120, 13, 191, 133)
            glassPaint.strokeWidth = 0.8f
            glassPaint.shader = null
            canvas.drawCircle(cx, cy, iconR, glassPaint)

            // Emoji text
            iconTxtPaint.textSize = iconR * 1.0f
            iconTxtPaint.alpha = 210
            canvas.drawText(iconsSubset[i], cx, cy + iconR * 0.36f, iconTxtPaint)
        }
    }
}