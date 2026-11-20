package app.sole.tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.sole.tracker.R
import app.sole.tracker.data.DayRecord
import app.sole.tracker.data.PrefsManager
import app.sole.tracker.data.StepDatabaseHelper
import app.sole.tracker.databinding.ActivityMainBinding
import app.sole.tracker.sensor.StepForegroundService
import app.sole.tracker.sensor.StepTracker
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var db: StepDatabaseHelper
    private val numFmt = NumberFormat.getNumberInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)

        if (!prefs.isOnboarded) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        db = StepDatabaseHelper(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupActions()
        StepForegroundService.syncServiceState(this, prefs.backgroundServiceEnabled)
    }

    override fun onResume() {
        super.onResume()
        if (!prefs.isOnboarded) return
        StepTracker.syncOnce(this) { refreshAllScreens() }
        refreshAllScreens()
    }

    private fun setupNavigation() {
        binding.navToday.setOnClickListener { selectTab(0) }
        binding.navWeek.setOnClickListener { selectTab(1) }
        binding.navTrends.setOnClickListener { selectTab(2) }
    }

    private fun selectTab(index: Int) {
        binding.pageToday.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.pageWeek.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.pageTrends.visibility = if (index == 2) View.VISIBLE else View.GONE

        val tabs = listOf(binding.navToday, binding.navWeek, binding.navTrends)
        tabs.forEachIndexed { idx, tv ->
            if (idx == index) {
                tv.setBackgroundResource(R.drawable.bg_nav_pill_active)
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                tv.background = null
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
        refreshAllScreens()
    }

    private fun setupActions() {
        binding.btnProfile.setOnClickListener {
            SettingsBottomSheet {
                refreshAllScreens()
            }.show(supportFragmentManager, "settings_sheet")
        }

        binding.btnLogActivity.setOnClickListener {
            LogActivityBottomSheet {
                refreshAllScreens()
            }.show(supportFragmentManager, "log_activity_sheet")
        }

        binding.weekBarChart.onDaySelected = { _, rec ->
            updateWeekSelectedDetail(rec)
        }

        binding.trendsSparkline.onPointSelected = { rec ->
            val d = LocalDate.parse(rec.dateIso)
            val dateLabel = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            binding.tvSparklineInspect.text =
                "$dateLabel · ${numFmt.format(rec.steps)} steps"
        }

        binding.monthlyGoalGrid.onDayDotClicked = { rec ->
            val d = LocalDate.parse(rec.dateIso)
            val dateLabel = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            binding.tvCalendarInspect.text =
                "$dateLabel · ${numFmt.format(rec.steps)} / ${numFmt.format(rec.goal)} steps · ${formatDistance(rec.totalDistanceKm(prefs))}"
        }
    }

    private fun refreshAllScreens() {
        val today = LocalDate.now()
        val todayRecord = db.getOrCreateDay(today.toString(), prefs.dailyStepGoal)

        // 1. TODAY SCREEN
        binding.tvTodayDate.text =
            today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US))

        val avatarUri = prefs.avatarUri
        if (!avatarUri.isNullOrBlank()) {
            binding.ivProfileAvatar.visibility = View.VISIBLE
            binding.tvProfileInitial.visibility = View.GONE
            binding.ivProfileAvatar.setImageURI(Uri.parse(avatarUri))
        } else {
            binding.ivProfileAvatar.visibility = View.GONE
            binding.tvProfileInitial.visibility = View.VISIBLE
            binding.tvProfileInitial.text =
                prefs.userName.firstOrNull()?.uppercase() ?: "N"
        }

        binding.stepRingView.setProgress(todayRecord.steps, todayRecord.goal)
        binding.tvStepCount.text = numFmt.format(todayRecord.steps)

        val remaining = (todayRecord.goal - todayRecord.steps).coerceAtLeast(0)
        binding.tvStepsRemaining.text = if (remaining > 0) {
            "${numFmt.format(remaining)} steps to go"
        } else {
            "Daily goal reached"
        }

        binding.tvDistanceValue.text = formatDistance(todayRecord.totalDistanceKm(prefs))
        binding.tvKcalValue.text = numFmt.format(todayRecord.totalKcal(prefs))
        binding.tvActiveValue.text = "${todayRecord.totalActiveMinutes()} min"

        // Completely hide AI Log Activity button when disabled or no API key
        binding.btnLogActivity.visibility =
            if (prefs.shouldShowAiFeatures()) View.VISIBLE else View.GONE

        // 2. WEEK SCREEN (Monday – Sunday)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val weekStartFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val weekEndFmt = if (monday.month == sunday.month) {
            DateTimeFormatter.ofPattern("d", Locale.US)
        } else {
            DateTimeFormatter.ofPattern("MMM d", Locale.US)
        }
        binding.tvWeekDateRange.text = "${monday.format(weekStartFmt)} – ${sunday.format(weekEndFmt)}"

        val weekRecords = db.getRange(monday, sunday, prefs.dailyStepGoal)
        val weekTotal = weekRecords.sumOf { it.steps }
        binding.tvWeekTotalSteps.text = numFmt.format(weekTotal)
        binding.tvWeekGoalLabel.text = "${numFmt.format(prefs.dailyStepGoal)} goal"

        val elapsedDays = (today.dayOfWeek.value).coerceIn(1, 7)
        val weekAvg = weekTotal / elapsedDays
        val bestDay = weekRecords.maxOfOrNull { it.steps } ?: 0
        val weekDistKm = weekRecords.sumOf { it.totalDistanceKm(prefs) }

        binding.tvWeekDailyAvg.text = numFmt.format(weekAvg)
        binding.tvWeekBestDay.text = numFmt.format(bestDay)
        binding.tvWeekDistance.text = formatDistance(weekDistKm)

        val todayIdx = (today.dayOfWeek.value - 1).coerceIn(0, 6)
        binding.weekBarChart.setData(weekRecords, prefs.dailyStepGoal, todayIdx)
        updateWeekSelectedDetail(weekRecords[todayIdx])

        // 3. TRENDS SCREEN (Last 30 days + Sunday-start monthly grid)
        val start30 = today.minusDays(29)
        val last30 = db.getRange(start30, today, prefs.dailyStepGoal)
        val avg30 = last30.sumOf { it.steps } / 30

        binding.tvTrendsDailyAvg.text = numFmt.format(avg30)
        binding.trendsSparkline.setData(last30)

        val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        binding.tvSparklineStartDate.text = start30.format(shortDate)
        binding.tvSparklineEndDate.text = today.format(shortDate)
        binding.tvSparklineInspect.text = ""

        val ym = YearMonth.from(today)
        binding.tvMonthName.text = ym.month.getDisplayName(TextStyle.FULL, Locale.US)
        val monthRecords = db.getRange(ym.atDay(1), ym.atEndOfMonth(), prefs.dailyStepGoal)
        binding.monthlyGoalGrid.setMonthData(ym, monthRecords)

        val goalsHit = monthRecords.count { it.isGoalHit() }
        var bestStreak = 0
        var curStreak = 0
        for (rec in monthRecords) {
            if (rec.isGoalHit()) {
                curStreak++
                if (curStreak > bestStreak) bestStreak = curStreak
            } else {
                curStreak = 0
            }
        }
        binding.tvMonthStats.text = "$goalsHit goals hit · best streak $bestStreak"
    }

    private fun updateWeekSelectedDetail(rec: DayRecord) {
        val d = LocalDate.parse(rec.dateIso)
        val dayName = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
        binding.tvWeekSelectedDetail.text =
            "$dayName · ${formatDistance(rec.totalDistanceKm(prefs))} · ${numFmt.format(rec.totalKcal(prefs))} Kcal · ${rec.totalActiveMinutes()} min"
    }

    private fun formatDistance(km: Double): String {
        return if (prefs.isImperial) {
            String.format(Locale.US, "%.1f mi", km * 0.621371)
        } else {
            String.format(Locale.US, "%.1f km", km)
        }
    }
}
