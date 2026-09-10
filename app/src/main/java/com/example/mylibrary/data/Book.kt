package com.example.mylibrary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(entity = Branch::class, parentColumns = ["branchId"], childColumns = ["branchId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Author::class, parentColumns = ["authorId"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("branchId"), Index("authorId")]
)

data class Book(
    @PrimaryKey(autoGenerate = true)
    val bookId: Int = 0,
    val title: String,
    val publicationYear: Int,
    val branchId: Int,
    val authorId: Int,
    val stock: Int,
    val genre: String,
    val timestamp: Long = System.currentTimeMillis()
)