package app.sole.tracker.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import app.sole.tracker.data.DayRecord
import java.text.NumberFormat
import java.util.Locale

class WeekBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    private var records: List<DayRecord> = emptyList()
    private var selectedIndex: Int = 0
    private var goalSteps: Int = 10_000

    var onDaySelected: ((Int, DayRecord) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141416")
        style = Paint.Style.FILL
    }

    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val unselectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A3A3E")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 5f * density), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A7A7E")
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }

    private val selectedLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * density
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val barRect = RectF()

    fun setData(weekRecords: List<DayRecord>, activeGoal: Int, initialSelectedIndex: Int) {
        records = weekRecords
        goalSteps = activeGoal.coerceAtLeast(1)
        selectedIndex = initialSelectedIndex.coerceIn(0, (weekRecords.size - 1).coerceAtLeast(0))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (records.isEmpty()) return

        val topPadding = 24f * density
        val bottomLabelSpace = 28f * density
        val chartTop = topPadding
        val chartBottom = height - bottomLabelSpace
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)

        val maxVal = maxOf(goalSteps * 1.18f, (records.maxOfOrNull { it.steps } ?: 0) * 1.12f)

        // Draw dashed goal line
        val goalY = chartBottom - (goalSteps / maxVal) * chartHeight
        canvas.drawLine(0f, goalY, width.toFloat(), goalY, dashedLinePaint)

        val slotWidth = width / 7f
        val barWidth = (slotWidth * 0.56f).coerceAtMost(32f * density)
        val cornerRadius = barWidth / 2f
        val formatter = NumberFormat.getNumberInstance(Locale.US)

        for (i in 0 until minOf(7, records.size)) {
            val rec = records[i]
            val cx = slotWidth * i + slotWidth / 2f
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f

            // Background full-height stadium track
            barRect.set(left, chartTop, right, chartBottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, trackPaint)

            if (rec.steps > 0) {
                val barH = ((rec.steps / maxVal) * chartHeight).coerceAtLeast(barWidth)
                val barTop = (chartBottom - barH).coerceAtLeast(chartTop)
                barRect.set(left, barTop, right, chartBottom)

                if (i == selectedIndex) {
                    canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, selectedBarPaint)
                    canvas.drawText(
                        formatter.format(rec.steps),
                        cx,
                        barTop - 8f * density,
                        selectedLabelPaint
                    )
                } else {
                    unselectedBarPaint.shader = LinearGradient(
                        0f, barTop, 0f, chartBottom,
                        Color.parseColor("#8E8E93"),
                        Color.parseColor("#323236"),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, unselectedBarPaint)
                }
            } else if (i == selectedIndex) {
                canvas.drawText("0", cx, chartBottom - 10f * density, selectedLabelPaint)
            }

            val dayTextPaint = if (i == selectedIndex) selectedLabelPaint else labelPaint
            canvas.drawText(dayLabels[i], cx, height - 6f * density, dayTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            if (records.isNotEmpty() && width > 0) {
                val idx = ((event.x / width) * 7f).toInt().coerceIn(0, records.size - 1)
                if (idx != selectedIndex) {
                    selectedIndex = idx
                    invalidate()
                    onDaySelected?.invoke(idx, records[idx])
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
