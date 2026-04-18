package com.calixyai.di

import android.content.Context
import androidx.room.Room
import com.calixyai.data.local.AppDatabase
import com.calixyai.data.local.dao.DietaryDao
import com.calixyai.data.local.dao.GoalDao
import com.calixyai.data.local.dao.OnboardingStateDao
import com.calixyai.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "calixyai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideDietaryDao(db: AppDatabase): DietaryDao = db.dietaryDao()
    @Provides fun provideOnboardingStateDao(db: AppDatabase): OnboardingStateDao = db.onboardingStateDao()
}
