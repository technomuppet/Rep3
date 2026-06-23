package com.replog.di

import android.content.Context
import com.replog.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.getDatabase(context)
    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideSetLogDao(db: AppDatabase): SetLogDao = db.setLogDao()
    @Provides fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()
    @Provides fun provideBodyweightDao(db: AppDatabase): BodyweightDao = db.bodyweightDao()
    @Provides fun providePrescriptionDao(db: AppDatabase): PrescriptionDao = db.prescriptionDao()
    @Provides fun provideTrainingDnaDao(db: AppDatabase): TrainingDnaDao = db.trainingDnaDao()
    @Provides fun provideKnowledgeGraphDao(db: AppDatabase): KnowledgeGraphDao = db.knowledgeGraphDao()
    @Provides fun provideRestLogDao(db: AppDatabase): RestLogDao = db.restLogDao()
    @Provides fun provideTrainingDnaSnapshotDao(db: AppDatabase): TrainingDnaSnapshotDao = db.trainingDnaSnapshotDao()
    @Provides fun provideTrainingDnaProgressionScoreDao(db: AppDatabase): TrainingDnaProgressionScoreDao = db.trainingDnaProgressionScoreDao()
    @Provides fun providePlateauEventDao(db: AppDatabase): PlateauEventDao = db.plateauEventDao()
    @Provides fun provideRecommendationHistoryDao(db: AppDatabase): RecommendationHistoryDao = db.recommendationHistoryDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
}
