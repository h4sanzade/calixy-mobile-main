package com.calixyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "onboarding_state")
data class OnboardingStateEntity(
    @PrimaryKey val id: Int = 1,
    val isOnboardingDone: Boolean = false,
    val isChatSetupDone: Boolean = false,
    val isPaymentShown: Boolean = false
)
