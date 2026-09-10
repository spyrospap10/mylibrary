package com.example.mylibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class Author(
    @PrimaryKey(autoGenerate = true)
    val authorId: Int = 0,
    val firstName: String,
    val lastName: String
)