package app.sole.tracker.data

data class DayRecord(
    val dateIso: String,       // yyyy-MM-dd
    val steps: Int,
    val goal: Int,             // Preserves original goal active on that date
    val bonusDistanceKm: Double = 0.0,
    val bonusKcal: Int = 0,
    val bonusActiveMin: Int = 0
) {
    fun totalDistanceKm(prefs: PrefsManager): Double {
        val strideMeters = if (prefs.useBodyMetrics && prefs.heightCm > 50f) {
            (prefs.heightCm * 0.415) / 100.0
        } else {
            0.75 // Lightweight standard stride
        }
        val stepDistKm = (steps * strideMeters) / 1000.0
        return stepDistKm + bonusDistanceKm
    }

    fun totalKcal(prefs: PrefsManager): Int {
        val kcalPerStep = if (prefs.useBodyMetrics && prefs.weightKg > 20f) {
            0.00057 * prefs.weightKg
        } else {
            0.0417 // Lightweight standard (~312 kcal at 7,482 steps)
        }
        return (steps * kcalPerStep).toInt() + bonusKcal
    }

    fun totalActiveMinutes(): Int {
        // ~155 steps per active minute baseline + logged active minutes
        return (steps / 155) + bonusActiveMin
    }

    fun isGoalHit(): Boolean = steps >= goal
}
