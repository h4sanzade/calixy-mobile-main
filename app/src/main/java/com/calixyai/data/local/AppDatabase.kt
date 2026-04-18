package com.calixyai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calixyai.data.local.dao.DietaryDao
import com.calixyai.data.local.dao.GoalDao
import com.calixyai.data.local.dao.OnboardingStateDao
import com.calixyai.data.local.dao.UserProfileDao
import com.calixyai.data.local.entity.DietaryEntity
import com.calixyai.data.local.entity.GoalEntity
import com.calixyai.data.local.entity.OnboardingStateEntity
import com.calixyai.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        GoalEntity::class,
        DietaryEntity::class,
        OnboardingStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao
    abstract fun dietaryDao(): DietaryDao
    abstract fun onboardingStateDao(): OnboardingStateDao
}
