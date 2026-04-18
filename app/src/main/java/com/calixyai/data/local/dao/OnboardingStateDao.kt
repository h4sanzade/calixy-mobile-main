package com.calixyai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calixyai.data.local.entity.OnboardingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: OnboardingStateEntity)

    @Query("SELECT * FROM onboarding_state WHERE id = 1")
    suspend fun getState(): OnboardingStateEntity?

    @Query("SELECT * FROM onboarding_state WHERE id = 1")
    fun observeState(): Flow<OnboardingStateEntity?>
}
