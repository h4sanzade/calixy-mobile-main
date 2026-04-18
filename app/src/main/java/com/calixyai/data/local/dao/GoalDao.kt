package com.calixyai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calixyai.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Query("SELECT * FROM goal WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): GoalEntity?

    @Query("SELECT * FROM goal WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: Long): Flow<GoalEntity?>
}
