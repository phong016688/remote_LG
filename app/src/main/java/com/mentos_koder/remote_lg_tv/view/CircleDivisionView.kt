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
    private var divisionLinePaint: Paint? = null
    private var divisionPath: Path? = null

    constructor(context: Context?) : super(context) {
        initializePaints()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initializePaints()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initializePaints()
    }

    private fun initializePaints() {
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint!!.color = Color.CYAN
        circlePaint!!.style = Paint.Style.FILL
        divisionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        divisionLinePaint!!.color = Color.GRAY
        divisionLinePaint!!.style = Paint.Style.STROKE
        divisionLinePaint!!.strokeWidth = 1f
        divisionPath = Path()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width
        val viewHeight = height
        val circleRadius = min(viewWidth, viewHeight) / 2
        canvas.drawCircle(viewWidth / 2f, viewHeight / 2f, circleRadius.toFloat(), circlePaint!!)
        val diagonalStartX = viewWidth / 2f - circleRadius * 0.707f // cos(45 degrees) ~ 0.707
        val diagonalStartY = viewHeight / 2f - circleRadius * 0.707f
        val diagonalEndX = viewWidth / 2f + circleRadius * 0.707f
        val diagonalEndY = viewHeight / 2f + circleRadius * 0.707f
        canvas.drawLine(diagonalStartX, diagonalStartY, diagonalEndX, diagonalEndY, divisionLinePaint!!)
        canvas.drawLine(diagonalEndX, diagonalStartY, diagonalStartX, diagonalEndY, divisionLinePaint!!)
    }
}

