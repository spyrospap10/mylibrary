package com.example.mylibrary.data

import androidx.room.*
import com.example.mylibrary.data.User

@Dao
interface UserDao {
    @Insert
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE username = :user AND password = :pass LIMIT 1")
    suspend fun login(user: String, pass: String): User?

    @Query("SELECT * FROM users WHERE username = :user LIMIT 1")
    suspend fun getUserByUsername(user: String): User?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}