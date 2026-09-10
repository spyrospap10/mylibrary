package com.example.mylibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mylibrary.data.AppDatabase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_login)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupRole)
        val radioAdmin = findViewById<RadioButton>(R.id.radioAdmin)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etBusinessCode = findViewById<EditText>(R.id.etBusinessCode)
        val tilBusinessCode = findViewById<View>(R.id.tilBusinessCode)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        val db = AppDatabase.getDatabase(this)

        val userDao = db.userDao()

        lifecycleScope.launch {
            val adminExists = userDao.getUserByUsername("a1")
            if (adminExists == null) {
                userDao.registerUser(
                    com.example.mylibrary.data.User(
                        firstName = "Βασίλης",
                        lastName = "Γεωργίου",
                        username = "a1",
                        email = "geo@gmail.com",
                        phone = "6900000000",
                        password = "user1",
                        role = "ADMIN",
                        businessCode = "12345"
                    )
                )
            }

            val userExists = userDao.getUserByUsername("spyrospap")
            if (userExists == null) {
                userDao.registerUser(
                    com.example.mylibrary.data.User(
                        firstName = "Σπύρος",
                        lastName = "Παπαδόπουλος",
                        username = "spyrospap",
                        email = "sp@gmail.com",
                        phone = "6900000001",
                        password = "user1",
                        role = "USER",
                        businessCode = null
                    )
                )
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioAdmin) {
                tilBusinessCode.visibility = View.VISIBLE
            } else {
                tilBusinessCode.visibility = View.GONE
            }
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val isAdmin = radioAdmin.isChecked
            val businessCode = etBusinessCode.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = userDao.getUserByUsername(username)

                if (user == null) {
                    Toast.makeText(this@LoginActivity, "Ο χρήστης δεν βρέθηκε. Κάντε εγγραφή.", Toast.LENGTH_LONG).show()
                } else if (user.password != password) {
                    Toast.makeText(this@LoginActivity, "Λάθος κωδικός πρόσβασης.", Toast.LENGTH_LONG).show()
                } else if (isAdmin && user.role != "ADMIN") {
                    Toast.makeText(this@LoginActivity, "Δεν έχετε δικαιώματα Διαχειριστή.", Toast.LENGTH_LONG).show()
                } else if (isAdmin && businessCode != "12345") {
                    Toast.makeText(this@LoginActivity, "Λάθος Κωδικός Επιχείρησης.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Επιτυχής Σύνδεση!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("USERNAME", user.username)
                    intent.putExtra("ROLE", user.role)
                    startActivity(intent)
                    finish()
                }
            }
        }

        btnSignup.setOnClickListener {
            etUsername.text.clear()
            etPassword.text.clear()
            etBusinessCode.text.clear()

            val radioAdmin = findViewById<RadioButton>(R.id.radioAdmin)
            val isAdmin = radioAdmin.isChecked
            val intent = Intent(this, SignupActivity::class.java)
            intent.putExtra("IS_ADMIN", isAdmin)
            startActivity(intent)
        }
    }
}