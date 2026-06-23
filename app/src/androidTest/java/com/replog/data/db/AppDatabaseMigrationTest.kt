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

    @Test
    fun migrate8To9_addsRestLogs() {
        // After reconciling Sprint 4 + Sprint 5, the 8->9 migration belongs to
        // Sprint 4 and creates the rest_logs table.
        helper.createDatabase(testDb, 8).apply {
            createVersion8Schema(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        assertTableExists(db, "rest_logs")
        assertIndexExists(db, "index_rest_logs_sessionExerciseId")
        assertIndexExists(db, "index_rest_logs_setId")
        assertIndexExists(db, "index_rest_logs_startedAt")
        db.close()
    }

    @Test
    fun migrate10To11_addsTrainingDnaAndRecommendationTables() {
        // Sprint 5's Training DNA + recommendation tables were relocated from the
        // original 8->9 migration to a fresh 10->11 migration during reconciliation.
        helper.createDatabase(testDb, 10).apply {
            createVersion10Schema(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDb,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )

        assertTableExists(db, "training_dna_snapshots")
        assertTableExists(db, "training_dna_progression_scores")
        assertTableExists(db, "plateau_events")
        assertTableExists(db, "recommendation_history")
        assertIndexExists(db, "index_training_dna_snapshots_generatedAt")
        assertIndexExists(db, "index_training_dna_progression_scores_exerciseId")
        assertIndexExists(db, "index_plateau_events_exerciseId")
        assertIndexExists(db, "index_recommendation_history_timestamp")
        assertIndexExists(db, "index_recommendation_history_recommendationType")
        db.close()
    }

    @Test
    fun migrate11To12_addsSessionRating() {
        helper.createDatabase(testDb, 11).apply {
            // Minimal workout_sessions shape as of v11 (scoring columns from 9->10).
            execSQL(
                "CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "templateName TEXT, startTime INTEGER NOT NULL, endTime INTEGER, notes TEXT, " +
                    "qualityScore INTEGER, totalVolume REAL NOT NULL DEFAULT 0, totalSets INTEGER NOT NULL DEFAULT 0, " +
                    "totalReps INTEGER NOT NULL DEFAULT 0, prCount INTEGER NOT NULL DEFAULT 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 12, true, AppDatabase.MIGRATION_11_12)
        assertColumnExists(db, "workout_sessions", "sessionRating")
        db.close()
    }

    @Test
    fun migrate12To13_addsGoalsTable() {
        helper.createDatabase(testDb, 12).apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "templateName TEXT, startTime INTEGER NOT NULL, endTime INTEGER, notes TEXT, " +
                    "qualityScore INTEGER, totalVolume REAL NOT NULL DEFAULT 0, totalSets INTEGER NOT NULL DEFAULT 0, " +
                    "totalReps INTEGER NOT NULL DEFAULT 0, prCount INTEGER NOT NULL DEFAULT 0, sessionRating INTEGER)"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(testDb, 13, true, AppDatabase.MIGRATION_12_13)
        assertTableExists(db, "goals")
        assertIndexExists(db, "index_goals_status")
        assertIndexExists(db, "index_goals_createdAt")
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

    private fun createVersion8Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, equipment TEXT NOT NULL, type TEXT NOT NULL, muscles TEXT NOT NULL, primaryMuscles TEXT NOT NULL, secondaryMuscles TEXT NOT NULL, movementPattern TEXT NOT NULL, difficulty TEXT NOT NULL, mediaAsset TEXT NOT NULL, isCustom INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateName TEXT, startTime INTEGER NOT NULL, endTime INTEGER, notes TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS session_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderIndex INTEGER NOT NULL, supersetGroup TEXT, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS set_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionExerciseId INTEGER NOT NULL, setNumber INTEGER NOT NULL, weight REAL NOT NULL, reps INTEGER NOT NULL, isBodyweight INTEGER NOT NULL, isPR INTEGER NOT NULL, timestamp INTEGER NOT NULL, setType TEXT NOT NULL, rpe REAL, tempo TEXT, FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isBuiltIn INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS template_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, defaultSets INTEGER NOT NULL, orderIndex INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL, FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS bodyweight_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, weight REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_prescriptions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, source TEXT NOT NULL, targetSets INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL, adjustment TEXT NOT NULL, reason TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS training_dna_metrics (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dimension TEXT NOT NULL, subjectType TEXT NOT NULL, subjectId TEXT, value REAL NOT NULL, confidence REAL NOT NULL, sampleSize INTEGER NOT NULL, updatedAt INTEGER NOT NULL, metadataJson TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS knowledge_graph_relations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sourceType TEXT NOT NULL, sourceId TEXT NOT NULL, targetType TEXT NOT NULL, targetId TEXT NOT NULL, relationType TEXT NOT NULL, strength REAL NOT NULL, evidenceCount INTEGER NOT NULL, firstSeenAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, metadataJson TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_dimension ON training_dna_metrics(dimension)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_subjectType ON training_dna_metrics(subjectType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_subjectId ON training_dna_metrics(subjectId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_training_dna_metrics_dimension_subjectType_subjectId ON training_dna_metrics(dimension, subjectType, subjectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceType ON knowledge_graph_relations(sourceType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceId ON knowledge_graph_relations(sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_targetType ON knowledge_graph_relations(targetType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_targetId ON knowledge_graph_relations(targetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_relationType ON knowledge_graph_relations(relationType)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceType_sourceId_targetType_targetId_relationType ON knowledge_graph_relations(sourceType, sourceId, targetType, targetId, relationType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_startTime ON workout_sessions(startTime)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_endTime ON workout_sessions(endTime)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_set_logs_timestamp ON set_logs(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_set_logs_sessionExerciseId_setNumber ON set_logs(sessionExerciseId, setNumber)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)")
    }

    private fun createVersion10Schema(db: SupportSQLiteDatabase) {
        // v8 baseline...
        createVersion8Schema(db)
        // ...plus 8->9 (rest_logs)...
        db.execSQL("CREATE TABLE IF NOT EXISTS rest_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionExerciseId INTEGER NOT NULL, setId INTEGER, plannedSeconds INTEGER NOT NULL, actualSeconds INTEGER, skipped INTEGER NOT NULL DEFAULT 0, startedAt INTEGER NOT NULL, completedAt INTEGER, FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(setId) REFERENCES set_logs(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_sessionExerciseId ON rest_logs(sessionExerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_setId ON rest_logs(setId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_startedAt ON rest_logs(startedAt)")
        // ...plus 9->10 (scoring columns, notes, PR type/completed flag).
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN qualityScore INTEGER")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalVolume REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalSets INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalReps INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN prCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE session_exercises ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN prType TEXT")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
    }

    private fun assertIndexExists(db: SupportSQLiteDatabase, indexName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name=?", arrayOf(indexName)).use { cursor ->
            check(cursor.moveToFirst()) { "Missing index $indexName" }
        }
    }

    private fun assertTableExists(db: SupportSQLiteDatabase, tableName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            check(cursor.moveToFirst()) { "Missing table $tableName" }
        }
    }

    private fun assertColumnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String) {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            var found = false
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIdx) == columnName) { found = true; break }
            }
            check(found) { "Missing column $columnName on $tableName" }
        }
    }
}
