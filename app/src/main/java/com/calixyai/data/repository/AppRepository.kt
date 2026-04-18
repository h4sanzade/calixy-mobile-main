package com.calixyai.data.repository

import com.calixyai.data.local.dao.DietaryDao
import com.calixyai.data.local.dao.GoalDao
import com.calixyai.data.local.dao.OnboardingStateDao
import com.calixyai.data.local.dao.UserProfileDao
import com.calixyai.data.local.entity.DietaryEntity
import com.calixyai.data.local.entity.GoalEntity
import com.calixyai.data.local.entity.OnboardingStateEntity
import com.calixyai.data.local.entity.UserProfileEntity
import com.calixyai.domain.model.SetupProfile
import com.calixyai.utils.toJsonString
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val goalDao: GoalDao,
    private val dietaryDao: DietaryDao,
    private val onboardingStateDao: OnboardingStateDao
) {
    fun observeOnboardingState(): Flow<OnboardingStateEntity?> = onboardingStateDao.observeState()
    fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observeLatest()

    suspend fun getOnboardingState(): OnboardingStateEntity = onboardingStateDao.getState() ?: OnboardingStateEntity()

    suspend fun setOnboardingDone() {
        onboardingStateDao.upsert(getOnboardingState().copy(isOnboardingDone = true))
    }

    suspend fun setChatSetupDone() {
        onboardingStateDao.upsert(getOnboardingState().copy(isChatSetupDone = true))
    }

    suspend fun setPaymentShown() {
        onboardingStateDao.upsert(getOnboardingState().copy(isPaymentShown = true))
    }

    suspend fun saveSetup(profile: SetupProfile) {
        val bmi = profile.bmi ?: 0f
        val userId = userProfileDao.insert(
            UserProfileEntity(
                firstName = profile.firstName,
                lastName = profile.lastName,
                gender = profile.gender,
                age = profile.age ?: 0,
                height = profile.heightCm ?: 0,
                weight = profile.weightKg ?: 0f,
                bmi = bmi
            )
        )
        val targetWeight = when (profile.goal) {
            "🔥 Lose Weight" -> (profile.weightKg ?: 0f) - 8f
            "💪 Build Muscle" -> (profile.weightKg ?: 0f) + 4f
            else -> profile.weightKg ?: 0f
        }
        val estimatedMonths = estimateMonths(bmi, profile.goal)
        val calories = estimateCalories(profile.goal, profile.activityLevel)
        goalDao.insert(
            GoalEntity(
                userId = userId,
                activityLevel = profile.activityLevel,
                goal = profile.goal,
                targetWeight = targetWeight,
                estimatedMonths = estimatedMonths,
                dailyCalories = calories
            )
        )
        dietaryDao.insert(
            DietaryEntity(
                userId = userId,
                allergies = profile.allergies.toJsonString(),
                dietaryRules = profile.dietaryRules.toJsonString(),
                customAllergy = profile.customAllergy
            )
        )
        setChatSetupDone()
    }

    private fun estimateCalories(goal: String, activity: String): Int {
        val base = when (activity) {
            "⚡ Athlete Mode" -> 2500
            "🏋️ Gym Regular" -> 2300
            "🏃 Moderately Active" -> 2150
            "🚶 Light Walker" -> 1950
            else -> 1800
        }
        return when (goal) {
            "🔥 Lose Weight" -> base - 300
            "💪 Build Muscle" -> base + 250
            else -> base
        }
    }

    private fun estimateMonths(bmi: Float, goal: String): Int {
        return when {
            goal == "💪 Build Muscle" -> 4
            bmi < 18.5f -> 3
            bmi < 25f -> 2
            bmi < 30f -> 4
            else -> 6
        }
    }
}
