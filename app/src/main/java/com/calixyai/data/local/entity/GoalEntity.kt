package com.calixyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val userId: Long,
    val activityLevel: String,
    val goal: String,
    val targetWeight: Float,
    val estimatedMonths: Int,
    val dailyCalories: Int
)
