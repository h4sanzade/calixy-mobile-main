package com.calixyai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calixyai.data.local.entity.DietaryEntity

@Dao
interface DietaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dietary: DietaryEntity)

    @Query("SELECT * FROM dietary WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): DietaryEntity?
}
