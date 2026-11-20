package app.sole.tracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class StepDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "sole_steps.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE daily_steps (
                date_iso TEXT PRIMARY KEY,
                steps INTEGER NOT NULL DEFAULT 0,
                goal INTEGER NOT NULL DEFAULT 10000,
                bonus_distance_km REAL NOT NULL DEFAULT 0.0,
                bonus_kcal INTEGER NOT NULL DEFAULT 0,
                bonus_active_min INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getOrCreateDay(dateIso: String, currentGoal: Int): DayRecord {
        val db = readableDatabase
        db.rawQuery(
            "SELECT date_iso, steps, goal, bonus_distance_km, bonus_kcal, bonus_active_min FROM daily_steps WHERE date_iso = ?",
            arrayOf(dateIso)
        ).use { c ->
            if (c.moveToFirst()) {
                return DayRecord(
                    dateIso = c.getString(0),
                    steps = c.getInt(1),
                    goal = c.getInt(2),
                    bonusDistanceKm = c.getDouble(3),
                    bonusKcal = c.getInt(4),
                    bonusActiveMin = c.getInt(5)
                )
            }
        }
        val fresh = DayRecord(dateIso = dateIso, steps = 0, goal = currentGoal)
        saveDay(fresh)
        return fresh
    }

    fun saveDay(record: DayRecord) {
        val values = ContentValues().apply {
            put("date_iso", record.dateIso)
            put("steps", record.steps)
            put("goal", record.goal)
            put("bonus_distance_km", record.bonusDistanceKm)
            put("bonus_kcal", record.bonusKcal)
            put("bonus_active_min", record.bonusActiveMin)
        }
        writableDatabase.insertWithOnConflict(
            "daily_steps",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateTodayGoal(todayIso: String, newGoal: Int) {
        val current = getOrCreateDay(todayIso, newGoal)
        saveDay(current.copy(goal = newGoal))
    }

    fun addSteps(dateIso: String, deltaSteps: Int, currentGoal: Int) {
        if (deltaSteps <= 0) return
        val current = getOrCreateDay(dateIso, currentGoal)
        saveDay(current.copy(steps = current.steps + deltaSteps))
    }

    fun addActivityMetrics(
        dateIso: String,
        currentGoal: Int,
        addSteps: Int,
        addDistKm: Double,
        addKcal: Int,
        addActiveMin: Int
    ) {
        val current = getOrCreateDay(dateIso, currentGoal)
        saveDay(
            current.copy(
                steps = current.steps + addSteps.coerceAtLeast(0),
                bonusDistanceKm = current.bonusDistanceKm + addDistKm.coerceAtLeast(0.0),
                bonusKcal = current.bonusKcal + addKcal.coerceAtLeast(0),
                bonusActiveMin = current.bonusActiveMin + addActiveMin.coerceAtLeast(0)
            )
        )
    }

    fun getRange(startDate: LocalDate, endDate: LocalDate, defaultGoal: Int): List<DayRecord> {
        val map = mutableMapOf<String, DayRecord>()
        readableDatabase.rawQuery(
            "SELECT date_iso, steps, goal, bonus_distance_km, bonus_kcal, bonus_active_min FROM daily_steps WHERE date_iso >= ? AND date_iso <= ?",
            arrayOf(startDate.toString(), endDate.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val rec = DayRecord(
                    dateIso = c.getString(0),
                    steps = c.getInt(1),
                    goal = c.getInt(2),
                    bonusDistanceKm = c.getDouble(3),
                    bonusKcal = c.getInt(4),
                    bonusActiveMin = c.getInt(5)
                )
                map[rec.dateIso] = rec
            }
        }
        val list = mutableListOf<DayRecord>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val iso = cursor.toString()
            list.add(map[iso] ?: DayRecord(dateIso = iso, steps = 0, goal = defaultGoal))
            cursor = cursor.plusDays(1)
        }
        return list
    }
}
