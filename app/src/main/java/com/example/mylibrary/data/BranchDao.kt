package com.example.mylibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Insert
    suspend fun insertBranch(branch: Branch)
    @Query("SELECT * FROM branches")
    fun getAllBranches(): Flow<List<Branch>>
}