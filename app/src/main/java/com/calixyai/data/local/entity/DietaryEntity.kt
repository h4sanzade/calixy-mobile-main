package com.calixyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dietary")
data class DietaryEntity(
    @PrimaryKey val userId: Long,
    val allergies: String,
    val dietaryRules: String,
    val customAllergy: String? = null
)
