package com.example.mylibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val message: String,
    val date: String,
    val isRead: Boolean = false
)