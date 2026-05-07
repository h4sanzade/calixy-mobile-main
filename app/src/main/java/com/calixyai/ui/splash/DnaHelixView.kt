package com.calixyai.ui.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

class DnaHelixView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintDot1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintDot2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBridge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private var phase = 0f
    private val speed = 0.035f

    private val dotCount = 36
    private val dotRadius = 7.5f
    private val amplitude = 28f

    private val runnable = object : Runnable {
        override fun run() {
            phase += speed
            invalidate()
            postDelayed(this, 16L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(runnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val cy = height / 2f

        for (i in 0 until dotCount) {
            val x = (i.toFloat() / (dotCount - 1)) * w
            val angle = (i.toFloat() / dotCount) * 2f * PI.toFloat() - phase

            val y1 = cy + sin(angle) * amplitude
            val y2 = cy + sin(angle + PI.toFloat()) * amplitude

            val alpha1 = (0.25f + 0.75f * ((sin(angle) + 1f) / 2f)).coerceIn(0f, 1f)
            val alpha2 = (0.25f + 0.75f * ((sin(angle + PI.toFloat()) + 1f) / 2f)).coerceIn(0f, 1f)

            // Bridge every 4th dot
            if (i % 4 == 0) {
                paintBridge.color = Color.argb(
                    (80 + alpha1 * 120).toInt().coerceIn(0, 255),
                    165, 214, 167
                )
                canvas.drawLine(x, y1, x, y2, paintBridge)
            }

            // Strand 1 — bright green
            paintDot1.color = Color.argb(
                (alpha1 * 255).toInt().coerceIn(0, 255),
                76, 175, 80
            )
            canvas.drawCircle(x, y1, dotRadius, paintDot1)

            // Strand 2 — lighter green
            paintDot2.color = Color.argb(
                (alpha2 * 200).toInt().coerceIn(0, 255),
                129, 199, 132
            )
            canvas.drawCircle(x, y2, dotRadius * 0.88f, paintDot2)
        }
    }
}