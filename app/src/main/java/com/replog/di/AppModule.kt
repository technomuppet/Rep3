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
}
