package com.replog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.KnowledgeGraphRelation
import com.replog.data.model.SessionExercise
import com.replog.data.model.SetLog
import com.replog.data.model.TemplateExercise
import com.replog.data.model.TrainingDnaMetric
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
        TrainingDnaMetric::class,
        KnowledgeGraphRelation::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun templateDao(): TemplateDao
    abstract fun bodyweightDao(): BodyweightDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun trainingDnaDao(): TrainingDnaDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao

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

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "replog_database")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                .also { INSTANCE = it }
        }
    }
}
