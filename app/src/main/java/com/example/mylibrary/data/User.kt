package com.example.mylibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String,
    val businessCode: String? = null,
    val isPremium: Boolean = false,
    val subscriptionEndDate: String = ""
)