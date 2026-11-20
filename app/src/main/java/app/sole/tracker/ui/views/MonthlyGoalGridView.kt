package app.sole.tracker.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import app.sole.tracker.data.DayRecord
import java.time.LocalDate
import java.time.YearMonth

class MonthlyGoalGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val headers = listOf("S", "M", "T", "W", "T", "F", "S")
    private var monthRecords: Map<Int, DayRecord> = emptyMap()
    private var yearMonth: YearMonth = YearMonth.now()
    private var today: LocalDate = LocalDate.now()

    var onDayDotClicked: ((DayRecord) -> Unit)? = null

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6E6E73")
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
    }

    private val hitDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val missedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222224")
        style = Paint.Style.FILL
    }

    private val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
    }

    private val hitCenters = mutableListOf<Triple<Float, Float, DayRecord>>()

    fun setMonthData(ym: YearMonth, records: List<DayRecord>) {
        yearMonth = ym
        today = LocalDate.now()
        monthRecords = records.associateBy { LocalDate.parse(it.dateIso).dayOfMonth }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        hitCenters.clear()

        val colW = width / 7f
        val headerH = 22f * density
        for (c in 0 until 7) {
            canvas.drawText(headers[c], colW * c + colW / 2f, 12f * density, headerPaint)
        }

        // Sunday = 0 .. Saturday = 6
        val firstDayDow = yearMonth.atDay(1).dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = firstDayDow + daysInMonth
        val rows = ((totalCells + 6) / 7).coerceAtLeast(5)
        val rowH = (height - headerH) / rows.toFloat()
        val radius = (minOf(colW, rowH) * 0.34f).coerceAtMost(13f * density)

        for (day in 1..daysInMonth) {
            val cellIdx = firstDayDow + (day - 1)
            val row = cellIdx / 7
            val col = cellIdx % 7
            val cx = colW * col + colW / 2f
            val cy = headerH + rowH * row + rowH / 2f

            val date = yearMonth.atDay(day)
            val rec = monthRecords[day] ?: DayRecord(date.toString(), 0, 10_000)
            hitCenters.add(Triple(cx, cy, rec))

            when {
                rec.isGoalHit() -> {
                    canvas.drawCircle(cx, cy, radius, hitDotPaint)
                }
                date == today -> {
                    canvas.drawCircle(cx, cy, radius, missedDotPaint)
                    canvas.drawCircle(cx, cy, radius, todayRingPaint)
                }
                else -> {
                    canvas.drawCircle(cx, cy, radius, missedDotPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val hit = hitCenters.minByOrNull { (cx, cy, _) ->
                val dx = event.x - cx
                val dy = event.y - cy
                dx * dx + dy * dy
            }
            if (hit != null) {
                onDayDotClicked?.invoke(hit.third)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
