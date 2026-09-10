package com.example.mylibrary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Branch::class, Author::class, Book::class, Notification::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao
    abstract fun authorDao(): AuthorDao
    abstract fun branchDao(): BranchDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "library_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    database.branchDao().insertBranch(Branch(name = "Αθήνα", address = "Ερμού 15, Αθήνα", phone = "2101234567"))
                    database.branchDao().insertBranch(Branch(name = "Θεσσαλονίκη", address = "Τσιμισκή 40, Θεσσαλονίκη", phone = "2310123456"))

                    database.authorDao().insertAuthor(Author(firstName = "Stephen", lastName = "King"))
                    database.authorDao().insertAuthor(Author(firstName = "J.K.", lastName = "Rowling"))
                    database.authorDao().insertAuthor(Author(firstName = "Ευγένιος", lastName = "Τριβιζάς"))
                }
            }
        }
    }
}