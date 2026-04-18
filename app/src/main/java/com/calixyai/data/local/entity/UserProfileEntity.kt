package com.calixyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val age: Int,
    val height: Int,
    val weight: Float,
    val bmi: Float,
    val createdAt: Long = System.currentTimeMillis()
)
