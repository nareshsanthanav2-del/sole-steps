package app.sole.tracker.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import app.sole.tracker.data.DayRecord

class TrendsSparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private var records: List<DayRecord> = emptyList()
    private var selectedIndex: Int = -1

    var onPointSelected: ((DayRecord) -> Unit)? = null

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1C1E")
        strokeWidth = 1f * density
    }

    private val midLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#161618")
        strokeWidth = 1f * density
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.6f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val path = Path()

    fun setData(last30Days: List<DayRecord>) {
        records = last30Days
        selectedIndex = (last30Days.size - 1).coerceAtLeast(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 12f * density

        canvas.drawLine(0f, 1f, w, 1f, borderPaint)
        canvas.drawLine(0f, h * 0.62f, w, h * 0.62f, midLinePaint)
        canvas.drawLine(0f, h - 1f, w, h - 1f, borderPaint)

        if (records.size < 2) return

        val maxSteps = (records.maxOfOrNull { it.steps } ?: 10_000).coerceAtLeast(1000)
        val drawW = w - pad * 2
        val drawH = h - pad * 2.4f

        val pts = records.mapIndexed { idx, rec ->
            val x = pad + (idx.toFloat() / (records.size - 1)) * drawW
            val norm = (rec.steps.toFloat() / maxSteps).coerceIn(0f, 1f)
            val y = (h - pad * 1.2f) - norm * drawH
            x to y
        }

        path.reset()
        path.moveTo(pts[0].first, pts[0].second)
        for (i in 0 until pts.size - 1) {
            val (x1, y1) = pts[i]
            val (x2, y2) = pts[i + 1]
            val cx = (x1 + x2) / 2f
            path.cubicTo(cx, y1, cx, y2, x2, y2)
        }
        canvas.drawPath(path, linePaint)

        val dotIdx = selectedIndex.coerceIn(0, pts.size - 1)
        canvas.drawCircle(pts[dotIdx].first, pts[dotIdx].second, 5f * density, dotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            if (records.size >= 2 && width > 0) {
                val idx = ((event.x / width) * (records.size - 1)).toInt()
                    .coerceIn(0, records.size - 1)
                if (idx != selectedIndex) {
                    selectedIndex = idx
                    invalidate()
                    onPointSelected?.invoke(records[idx])
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
