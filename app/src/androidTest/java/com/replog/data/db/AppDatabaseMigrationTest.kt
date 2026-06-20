package com.replog.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate6To7_addsPerformanceIndexes() {
        helper.createDatabase(testDb, 6).apply {
            createVersion6Schema(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDb,
            7,
            true,
            AppDatabase.MIGRATION_6_7
        )

        assertIndexExists(db, "index_workout_sessions_startTime")
        assertIndexExists(db, "index_workout_sessions_endTime")
        assertIndexExists(db, "index_set_logs_timestamp")
        assertIndexExists(db, "index_set_logs_sessionExerciseId_setNumber")
        assertIndexExists(db, "index_exercises_name")
        db.close()
    }

    private fun createVersion6Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, equipment TEXT NOT NULL, type TEXT NOT NULL, muscles TEXT NOT NULL, primaryMuscles TEXT NOT NULL, secondaryMuscles TEXT NOT NULL, movementPattern TEXT NOT NULL, difficulty TEXT NOT NULL, mediaAsset TEXT NOT NULL, isCustom INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateName TEXT, startTime INTEGER NOT NULL, endTime INTEGER, notes TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS session_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderIndex INTEGER NOT NULL, supersetGroup TEXT, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS set_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionExerciseId INTEGER NOT NULL, setNumber INTEGER NOT NULL, weight REAL NOT NULL, reps INTEGER NOT NULL, isBodyweight INTEGER NOT NULL, isPR INTEGER NOT NULL, timestamp INTEGER NOT NULL, setType TEXT NOT NULL, rpe REAL, tempo TEXT, FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isBuiltIn INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS template_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, defaultSets INTEGER NOT NULL, orderIndex INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL, FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS bodyweight_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, weight REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_prescriptions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, source TEXT NOT NULL, targetSets INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL, adjustment TEXT NOT NULL, reason TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercises_sessionId ON session_exercises(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercises_exerciseId ON session_exercises(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercises_supersetGroup ON session_exercises(supersetGroup)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_set_logs_sessionExerciseId ON set_logs(sessionExerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_templateId ON template_exercises(templateId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseId ON template_exercises(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_prescriptions_sessionId ON workout_prescriptions(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_prescriptions_exerciseId ON workout_prescriptions(exerciseId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_prescriptions_sessionId_exerciseId ON workout_prescriptions(sessionId, exerciseId)")
    }

    private fun assertIndexExists(db: SupportSQLiteDatabase, indexName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name=?", arrayOf(indexName)).use { cursor ->
            check(cursor.moveToFirst()) { "Missing index $indexName" }
        }
    }
}
