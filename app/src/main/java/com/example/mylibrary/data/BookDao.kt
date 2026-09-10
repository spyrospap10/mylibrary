package com.example.mylibrary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert
    suspend fun insertBook(book: Book)

    @Query("SELECT * FROM books ORDER BY bookId DESC")
    suspend fun getAllBooks(): List<Book>

    @Query("SELECT * FROM books WHERE branchId = :branchId")
    fun getBooksByBranch(branchId: Int): Flow<List<Book>>

    @Query("UPDATE books SET stock = stock - 1 WHERE bookId = :bookId AND stock > 0")
    suspend fun decrementStock(bookId: Int): Int

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)
}