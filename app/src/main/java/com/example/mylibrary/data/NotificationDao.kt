package com.example.mylibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE username = :username ORDER BY id DESC")
    fun getUserNotifications(username: String): Flow<List<Notification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE username = :username AND isRead = 0")
    fun getUnreadCount(username: String): Flow<Int>

    @Insert
    suspend fun insertNotification(notification: Notification)

    @Update
    suspend fun updateNotification(notification: Notification)
}