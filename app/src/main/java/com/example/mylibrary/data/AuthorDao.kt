package com.example.mylibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AuthorDao {
    @Query("SELECT * FROM authors")
    suspend fun getAllAuthors(): List<Author>

    @Insert
    suspend fun insertAuthor(author: Author)
}