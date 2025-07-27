package com.mentos_koder.remote_lg_tv.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.min


class CircleDivisionView : View {
    private var circlePaint: Paint? = null
    private var linePaint: Paint? = null
    private var path: Path? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint!!.color = Color.CYAN
        circlePaint!!.style = Paint.Style.FILL
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint!!.color = Color.GRAY
        linePaint!!.style = Paint.Style.STROKE
        linePaint!!.strokeWidth = 1f
        path = Path()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        val radius = min(width, height) / 2
        canvas.drawCircle(width / 2f, height / 2f, radius.toFloat(), circlePaint!!)
        val startX = width / 2f - radius * 0.707f // cos(45 degrees) ~ 0.707
        val startY = height / 2f - radius * 0.707f
        val endX = width / 2f + radius * 0.707f
        val endY = height / 2f + radius * 0.707f
        canvas.drawLine(startX, startY, endX, endY, linePaint!!)
        canvas.drawLine(endX, startY, startX, endY, linePaint!!)
    }
}

