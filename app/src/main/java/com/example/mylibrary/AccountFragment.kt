package com.example.mylibrary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccountFragment : Fragment() {

    private lateinit var db: AppDatabase
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUser: User? = null
    private var isAdmin = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        db = AppDatabase.getDatabase(requireContext())

        val currentUsername = requireActivity().intent.getStringExtra("USERNAME") ?: ""
        val role = requireActivity().intent.getStringExtra("ROLE") ?: "USER"
        isAdmin = (role == "ADMIN")

        val etUsername = view.findViewById<EditText>(R.id.etAccUsername)
        val etFirstName = view.findViewById<EditText>(R.id.etAccFirstName)
        val etLastName = view.findViewById<EditText>(R.id.etAccLastName)
        val etPhone = view.findViewById<EditText>(R.id.etAccPhone)
        val etEmail = view.findViewById<EditText>(R.id.etAccEmail)

        val etOldPass = view.findViewById<EditText>(R.id.etAccOldPassword)
        val etNewPass = view.findViewById<EditText>(R.id.etAccNewPassword)
        val etConfirmPass = view.findViewById<EditText>(R.id.etAccConfirmPassword)

        val ivEdit = view.findViewById<ImageView>(R.id.ivEditProfile)

        ivEdit.setOnClickListener {
            etFirstName.isEnabled = true
            etLastName.isEnabled = true
            etPhone.isEnabled = true
            etEmail.isEnabled = true
            etFirstName.requestFocus()
            Toast.makeText(requireContext(), "Τώρα μπορείτε να επεξεργαστείτε τα στοιχεία σας", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val user = db.userDao().getUserByUsername(currentUsername)
            if (user != null) {
                currentUser = user
                etUsername.setText(user.username)
                etFirstName.setText(user.firstName)
                etLastName.setText(user.lastName)
                etPhone.setText(user.phone)
                etEmail.setText(user.email)
            }
        }

        view.findViewById<Button>(R.id.btnAccSave).setOnClickListener {
            val newFirstName = etFirstName.text.toString().trim()
            val newLastName = etLastName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()

            val oldPass = etOldPass.text.toString()
            val newPass = etNewPass.text.toString()
            val confirmPass = etConfirmPass.text.toString()

            currentUser?.let { user ->
                var finalPassword = user.password

                if (oldPass.isNotEmpty() || newPass.isNotEmpty() || confirmPass.isNotEmpty()) {
                    if (oldPass != user.password) {
                        Toast.makeText(requireContext(), "Ο τρέχων κωδικός είναι λάθος!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (newPass != confirmPass) {
                        Toast.makeText(requireContext(), "Οι νέοι κωδικοί δεν ταιριάζουν!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (newPass.length < 4) {
                        Toast.makeText(requireContext(), "Ο νέος κωδικός είναι πολύ μικρός!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    finalPassword = newPass
                }

                val updatedUser = user.copy(
                    firstName = newFirstName,
                    lastName = newLastName,
                    phone = newPhone,
                    email = newEmail,
                    password = finalPassword
                )

                lifecycleScope.launch {
                    db.userDao().updateUser(updatedUser)
                    Toast.makeText(requireContext(), "Τα στοιχεία αποθηκεύτηκαν!", Toast.LENGTH_SHORT).show()

                    etOldPass.text.clear()
                    etNewPass.text.clear()
                    etConfirmPass.text.clear()

                    etFirstName.isEnabled = false
                    etLastName.isEnabled = false
                    etPhone.isEnabled = false
                    etEmail.isEnabled = false
                }
            }
        }

        view.findViewById<Button>(R.id.btnAccDelete).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Διαγραφή Λογαριασμού")
                .setMessage("Είστε σίγουροι; Αυτή η ενέργεια είναι οριστική.")
                .setPositiveButton("Ναι, Διαγραφή") { _, _ ->
                    if (isAdmin) {
                        executeAccountDeletion()
                    } else {
                        checkObligationsAndDelete(currentUsername)
                    }
                }
                .setNegativeButton("Ακύρωση", null)
                .show()
        }

        return view
    }

    private fun checkObligationsAndDelete(username: String) {
        firestore.collection("Loans")
            .whereEqualTo("username", username)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener { loanDocs ->
                if (!loanDocs.isEmpty) {
                    Toast.makeText(requireContext(), "Αδύνατη διαγραφή: Έχετε βιβλία που δεν έχετε επιστρέψει!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                firestore.collection("Subscriptions")
                    .whereEqualTo("username", username)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .addOnSuccessListener { subDocs ->
                        if (!subDocs.isEmpty) {
                            Toast.makeText(requireContext(), "Αδύνατη διαγραφή: Έχετε ενεργή συνδρομή! Τερματίστε την πρώτα.", Toast.LENGTH_LONG).show()
                        } else {
                            executeAccountDeletion()
                        }
                    }
            }
    }

    private fun executeAccountDeletion() {
        lifecycleScope.launch {
            currentUser?.let { db.userDao().deleteUser(it) }
            Toast.makeText(requireContext(), "Ο λογαριασμός σας διαγράφηκε.", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}