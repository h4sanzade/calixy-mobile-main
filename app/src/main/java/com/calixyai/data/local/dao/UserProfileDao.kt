package com.calixyai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calixyai.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity): Long

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): UserProfileEntity?
}
