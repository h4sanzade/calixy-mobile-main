package com.calixyai.ui.onboarding

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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

    // ── Shared ─────────────────────────────────────────────────────────────
    private var tick = 0f

    // ── Page 0: Orbit ───────────────────────────────────────────────────────
    private data class OrbRing(val radiusFactor: Float, val speed: Float, val dotCount: Int, val colorA: Int, val colorB: Int)
    private val orbRings = listOf(
        OrbRing(0.18f,  0.012f, 4, Color.parseColor("#81C784"), Color.parseColor("#4CAF50")),
        OrbRing(0.30f, -0.008f, 6, Color.parseColor("#66BB6A"), Color.parseColor("#388E3C")),
        OrbRing(0.42f,  0.005f, 8, Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
    )
    private val paintOrb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f
    }

    // ── Page 1: Bars ────────────────────────────────────────────────────────
    private data class Bar(val label: String, val target: Float, val color: Int)
    private val bars = listOf(
        Bar("Protein", 0.78f, Color.parseColor("#4CAF50")),
        Bar("Carbs",   0.52f, Color.parseColor("#66BB6A")),
        Bar("Fats",    0.35f, Color.parseColor("#A5D6A7")),
        Bar("Fiber",   0.61f, Color.parseColor("#2E7D32")),
    )
    private var barProgress = 0f
    private val paintBar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBarBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF"); textAlign = Paint.Align.CENTER; textSize = 28f
    }
    private val paintValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8F5E9"); textAlign = Paint.Align.CENTER; textSize = 30f; isFakeBoldText = true
    }

    // ── Page 2: Chat bubbles ────────────────────────────────────────────────
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

    // ── Animation loop ──────────────────────────────────────────────────────
    private val frameRunnable = object : Runnable {
        override fun run() {
            tick += 1f
            when (pageIndex) {
                1 -> if (barProgress < 1f) barProgress = min(1f, barProgress + 0.022f)
                2 -> if (tick % 42 == 0f && chatStep < bubbles.size) chatStep++
            }
            invalidate()
            postDelayed(this, 16L)
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

    // ── Draw dispatch ───────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (pageIndex) {
            0 -> drawOrbit(canvas)
            1 -> drawBars(canvas)
            2 -> drawChat(canvas)
        }
    }

    // ── Page 0: Orbit ───────────────────────────────────────────────────────
    private fun drawOrbit(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val minDim = min(width, height).toFloat()

        orbRings.forEach { ring ->
            val radius = minDim * ring.radiusFactor
            paintRing.color = Color.argb(30, 76, 175, 80)
            canvas.drawCircle(cx, cy, radius, paintRing)

            for (i in 0 until ring.dotCount) {
                val angle = (i.toFloat() / ring.dotCount) * 2f * PI.toFloat() + tick * ring.speed
                val dx = cx + cos(angle) * radius
                val dy = cy + sin(angle) * radius
                val alpha = ((sin(angle + tick * 0.03f) + 1f) / 2f)
                paintOrb.color = Color.argb(
                    (100 + alpha * 155).toInt().coerceIn(0, 255),
                    Color.red(ring.colorA),
                    Color.green(ring.colorA),
                    Color.blue(ring.colorA)
                )
                canvas.drawCircle(dx, dy, minDim * 0.014f, paintOrb)
            }
        }

        // Centre dot
        val pulse = 1f + 0.06f * sin(tick * 0.06f)
        val cR = minDim * 0.085f * pulse
        paintOrb.color = Color.parseColor("#1B5E20")
        canvas.drawCircle(cx, cy, cR, paintOrb)
        paintRing.color = Color.parseColor("#4CAF50")
        paintRing.strokeWidth = 2f
        canvas.drawCircle(cx, cy, cR, paintRing)
        paintRing.strokeWidth = 1f

        // Leaf emoji via text
        paintLabel.textSize = minDim * 0.09f
        paintLabel.color = Color.parseColor("#E8F5E9")
        paintLabel.textAlign = Paint.Align.CENTER
        canvas.drawText("🌿", cx, cy + minDim * 0.032f, paintLabel)
    }

    // ── Page 1: Macro bars ──────────────────────────────────────────────────
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

            // Background track
            paintBarBg.color = Color.argb(30, 76, 175, 80)
            canvas.drawRoundRect(rect, 12f, 12f, paintBarBg)

            // Filled portion
            val fillRect = RectF(x, baseY - filledH, x + barW, baseY)
            paintBar.color = bar.color
            canvas.drawRoundRect(fillRect, 12f, 12f, paintBar)

            // Percentage value
            val pct = (bar.target * barProgress * 100).toInt()
            if (filledH > 24f) {
                paintValue.textSize = barW * 0.38f
                canvas.drawText("$pct%", x + barW / 2f, baseY - filledH - 10f, paintValue)
            }

            // Label
            paintLabel.textSize = barW * 0.28f
            paintLabel.color = Color.argb(140, 255, 255, 255)
            canvas.drawText(bar.label, x + barW / 2f, baseY + 28f, paintLabel)
        }
    }

    // ── Page 2: Chat bubbles ────────────────────────────────────────────────
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
                // Bot border
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