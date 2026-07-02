package com.replog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.Goal
import com.replog.data.model.PlateauEvent
import com.replog.data.model.RecommendationHistory
import com.replog.data.model.RestDayOverride
import com.replog.data.model.RestLog
import com.replog.data.model.SessionExercise
import com.replog.data.model.SetLog
import com.replog.data.model.TemplateExercise
import com.replog.data.model.TrainingDnaProgressionScore
import com.replog.data.model.TrainingDnaSnapshot
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.model.WorkoutTemplate

@Database(
    entities = [
        Exercise::class,
        WorkoutSession::class,
        SessionExercise::class,
        SetLog::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        BodyweightLog::class,
        WorkoutPrescription::class,
        RestLog::class,
        TrainingDnaSnapshot::class,
        TrainingDnaProgressionScore::class,
        PlateauEvent::class,
        RecommendationHistory::class,
        Goal::class,
        RestDayOverride::class
    ],
    version = 16,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun templateDao(): TemplateDao
    abstract fun bodyweightDao(): BodyweightDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun restLogDao(): RestLogDao
    abstract fun trainingDnaSnapshotDao(): TrainingDnaSnapshotDao
    abstract fun trainingDnaProgressionScoreDao(): TrainingDnaProgressionScoreDao
    abstract fun plateauEventDao(): PlateauEventDao
    abstract fun recommendationHistoryDao(): RecommendationHistoryDao
    abstract fun goalDao(): GoalDao
    abstract fun restDayOverrideDao(): RestDayOverrideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bodyweight_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weight REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        note TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN primaryMuscles TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exercises ADD COLUMN secondaryMuscles TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exercises ADD COLUMN movementPattern TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exercises ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Intermediate'")
                database.execSQL("ALTER TABLE set_logs ADD COLUMN setType TEXT NOT NULL DEFAULT 'Working'")
                database.execSQL("ALTER TABLE set_logs ADD COLUMN rpe REAL")
                database.execSQL("ALTER TABLE set_logs ADD COLUMN tempo TEXT")
                database.execSQL("ALTER TABLE session_exercises ADD COLUMN supersetGroup TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercises_supersetGroup ON session_exercises(supersetGroup)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN mediaAsset TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE template_exercises ADD COLUMN targetReps INTEGER NOT NULL DEFAULT 8")
                database.execSQL("ALTER TABLE template_exercises ADD COLUMN targetWeight REAL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_prescriptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        targetSets INTEGER NOT NULL,
                        targetReps INTEGER NOT NULL,
                        targetWeight REAL,
                        adjustment TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_workout_prescriptions_sessionId ON workout_prescriptions(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_workout_prescriptions_exerciseId ON workout_prescriptions(exerciseId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_prescriptions_sessionId_exerciseId ON workout_prescriptions(sessionId, exerciseId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_startTime ON workout_sessions(startTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_endTime ON workout_sessions(endTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_set_logs_timestamp ON set_logs(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_set_logs_sessionExerciseId_setNumber ON set_logs(sessionExerciseId, setNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)")
            }
        }


        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_dna_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dimension TEXT NOT NULL,
                        subjectType TEXT NOT NULL,
                        subjectId TEXT,
                        value REAL NOT NULL,
                        confidence REAL NOT NULL,
                        sampleSize INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        metadataJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_dimension ON training_dna_metrics(dimension)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_subjectType ON training_dna_metrics(subjectType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_metrics_subjectId ON training_dna_metrics(subjectId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_training_dna_metrics_dimension_subjectType_subjectId ON training_dna_metrics(dimension, subjectType, subjectId)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS knowledge_graph_relations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sourceType TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        targetType TEXT NOT NULL,
                        targetId TEXT NOT NULL,
                        relationType TEXT NOT NULL,
                        strength REAL NOT NULL,
                        evidenceCount INTEGER NOT NULL,
                        firstSeenAt INTEGER NOT NULL,
                        lastSeenAt INTEGER NOT NULL,
                        metadataJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceType ON knowledge_graph_relations(sourceType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceId ON knowledge_graph_relations(sourceId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_targetType ON knowledge_graph_relations(targetType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_targetId ON knowledge_graph_relations(targetId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_graph_relations_relationType ON knowledge_graph_relations(relationType)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_knowledge_graph_relations_sourceType_sourceId_targetType_targetId_relationType ON knowledge_graph_relations(sourceType, sourceId, targetType, targetId, relationType)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rest_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionExerciseId INTEGER NOT NULL,
                        setId INTEGER,
                        plannedSeconds INTEGER NOT NULL,
                        actualSeconds INTEGER,
                        skipped INTEGER NOT NULL DEFAULT 0,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(setId) REFERENCES set_logs(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_sessionExerciseId ON rest_logs(sessionExerciseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_setId ON rest_logs(setId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rest_logs_startedAt ON rest_logs(startedAt)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // WorkoutSession scoring fields
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN qualityScore INTEGER")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalVolume REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalSets INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN totalReps INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN prCount INTEGER NOT NULL DEFAULT 0")
                // SessionExercise notes
                database.execSQL("ALTER TABLE session_exercises ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                // SetLog PR types + completed flag
                database.execSQL("ALTER TABLE set_logs ADD COLUMN prType TEXT")
                database.execSQL("ALTER TABLE set_logs ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
            }
        }

        // --- Sprint 5 (Training DNA + Recommendation engine) tables ---
        // Sprint 5 originally created these under MIGRATION_8_9 because it branched
        // from the v8 base. After reconciling with Sprint 4 (which owns 8->9 and
        // 9->10), the Sprint 5 tables are relocated to a fresh 10->11 migration so
        // every prior version upgrades cleanly. Note: Sprint 5 declared the
        // RecommendationHistory entity but never created its table in any migration;
        // that latent defect is fixed here by creating recommendation_history too.
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_dna_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        generatedAt INTEGER NOT NULL,
                        preferredRepRange TEXT NOT NULL,
                        preferredVolumeRange TEXT NOT NULL,
                        preferredFrequency TEXT NOT NULL,
                        strongestMuscles TEXT NOT NULL,
                        weakestMuscles TEXT NOT NULL,
                        fastestProgressingExercises TEXT NOT NULL,
                        stalledExercises TEXT NOT NULL,
                        averageWorkoutDuration REAL NOT NULL,
                        averageRecoveryHours REAL NOT NULL,
                        monthlyPRCount INTEGER NOT NULL,
                        volumeToleranceScore REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_snapshots_generatedAt ON training_dna_snapshots(generatedAt)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_dna_progression_scores (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        calculatedAt INTEGER NOT NULL,
                        score30Day REAL NOT NULL,
                        score90Day REAL NOT NULL,
                        scoreLifetime REAL NOT NULL,
                        estimatedOneRm30Day REAL NOT NULL,
                        estimatedOneRm90Day REAL NOT NULL,
                        estimatedOneRmLifetime REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_progression_scores_exerciseId ON training_dna_progression_scores(exerciseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_training_dna_progression_scores_calculatedAt ON training_dna_progression_scores(calculatedAt)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plateau_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        exerciseName TEXT NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        periodDays INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        lastLoad REAL NOT NULL,
                        lastReps INTEGER NOT NULL,
                        lastVolume REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_plateau_events_exerciseId ON plateau_events(exerciseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_plateau_events_detectedAt ON plateau_events(detectedAt)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recommendation_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        recommendationType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        dataUsed TEXT NOT NULL,
                        reasoning TEXT NOT NULL,
                        expectedOutcome TEXT NOT NULL,
                        confidenceScore REAL NOT NULL,
                        estimatedDurationMinutes INTEGER NOT NULL,
                        workoutSplit TEXT,
                        outcome TEXT,
                        rejectedReason TEXT
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_history_timestamp ON recommendation_history(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_history_recommendationType ON recommendation_history(recommendationType)")
            }
        }

        // Session Rating (#10): user's post-workout 1–5 rating on WorkoutSession.
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workout_sessions ADD COLUMN sessionRating INTEGER")
            }
        }

        // Goal Engine: user training goals.
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        exerciseId INTEGER,
                        exerciseName TEXT,
                        targetValue REAL NOT NULL,
                        targetReps INTEGER,
                        startValue REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        targetDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'active',
                        achievedAt INTEGER
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_status ON goals(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_createdAt ON goals(createdAt)")
            }
        }

        // Sprint 4: record "Train Anyway" rest-day overrides for future recovery intelligence.
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rest_day_overrides (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        recoveryScore INTEGER,
                        recommendationReason TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        // Sprint 5 P5: favourite/pinned templates for the Home quick-launch row.
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workout_templates ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Sprint 19: drop two abandoned prototype tables (never read/written by any
        // live feature - see ARCHITECTURE_CLEANUP_REPORT). The knowledge-graph and
        // DNA-signal-metric subsystems were superseded by ExerciseSwapEngine/
        // MuscleGapAnalyzer/RecommendationEngine and the live TrainingDnaEngine
        // (snapshots/scores/plateaus) respectively. Dropping is safe: no user data
        // is produced into these tables by any shipped code path.
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS training_dna_metrics")
                database.execSQL("DROP TABLE IF EXISTS knowledge_graph_relations")
            }
        }

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "replog_database")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                    MIGRATION_15_16
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}
