package com.example.mylibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.User
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etBusinessCode = findViewById<EditText>(R.id.etBusinessCode)
        val tilBusinessCode = findViewById<View>(R.id.tilBusinessCode)
        val btnCompleteSignup = findViewById<Button>(R.id.btnCompleteSignup)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        val db = AppDatabase.getDatabase(this)

        val userDao = db.userDao()

        if (isAdmin) {
            tilBusinessCode.visibility = View.VISIBLE
        } else {
            tilBusinessCode.visibility = View.GONE
        }

        btnCompleteSignup.setOnClickListener {
            val lastName = findViewById<EditText>(R.id.etLastName).text.toString().trim()
            val firstName = findViewById<EditText>(R.id.etFirstName).text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val businessCode = etBusinessCode.text.toString().trim()

            if (lastName.isEmpty() || firstName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.contains("@")) {
                Toast.makeText(this, "Το e-mail πρέπει να περιέχει @", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.isEmpty() || !phone.all { it.isDigit() }) {
                Toast.makeText(this, "Το τηλέφωνο πρέπει να περιέχει μόνο αριθμούς", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Οι κωδικοί δεν ταιριάζουν!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (isAdmin && businessCode != "12345") {
                Toast.makeText(this, "Λάθος Κωδικός Επιχείρησης! Αδύνατη η εγγραφή.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val roleToSave = if (isAdmin) "ADMIN" else "USER"
            val codeToSave = if (isAdmin) businessCode else null

            val newUser = User(
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                phone = phone,
                password = password,
                role = roleToSave,
                businessCode = codeToSave
            )

            lifecycleScope.launch {
                val existingUser = userDao.getUserByUsername(username)

                if (existingUser != null) {
                    Toast.makeText(this@SignupActivity, "Το όνομα χρήστη υπάρχει ήδη! Επιλέξτε άλλο.", Toast.LENGTH_LONG).show()
                } else {
                    userDao.registerUser(newUser)
                    Toast.makeText(this@SignupActivity, "Η εγγραφή ολοκληρώθηκε! Παρακαλώ συνδεθείτε.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}