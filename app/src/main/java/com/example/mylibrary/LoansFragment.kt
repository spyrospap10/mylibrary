package com.example.mylibrary

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.Book
import com.example.mylibrary.data.Loan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoansFragment : Fragment() {

    private lateinit var loanAdapter: LoanAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase
    private var isAdmin = false
    private var currentUsername = ""
    private var isHistoryMode = false
    private lateinit var recyclerViewLoans: RecyclerView
    private lateinit var tvEmptyLoans: TextView
    private lateinit var btnActiveLoans: Button
    private lateinit var btnHistoryLoans: Button
    private var currentLoansList: List<Loan> = emptyList()
    private var currentSearchQuery = ""
    private var selectedSortIndex = 0
    private var selectedDeliveryFilter = "Όλοι"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_loans, container, false)

        firestore = FirebaseFirestore.getInstance()
        db = AppDatabase.getDatabase(requireContext())

        val userRole = requireActivity().intent.getStringExtra("ROLE") ?: "USER"
        isAdmin = (userRole == "ADMIN")
        currentUsername = requireActivity().intent.getStringExtra("USERNAME") ?: "Άγνωστος"

        recyclerViewLoans = view.findViewById(R.id.recyclerViewLoans)
        tvEmptyLoans = view.findViewById(R.id.tvEmptyLoans)
        btnActiveLoans = view.findViewById(R.id.btnActiveLoans)
        btnHistoryLoans = view.findViewById(R.id.btnHistoryLoans)

        recyclerViewLoans.layoutManager = LinearLayoutManager(requireContext())

        loanAdapter = LoanAdapter(
            isAdmin = isAdmin,
            onReturnClick = { loan -> returnBook(loan) }
        )
        recyclerViewLoans.adapter = loanAdapter

        val layoutAdminFilters = view.findViewById<LinearLayout>(R.id.layoutAdminFilters)
        val layoutUserTabs = view.findViewById<LinearLayout>(R.id.layoutUserTabs)

        if (isAdmin) {
            layoutAdminFilters.visibility = View.VISIBLE
            layoutUserTabs.visibility = View.GONE

            val etSearchLoan = view.findViewById<EditText>(R.id.etSearchLoan)
            etSearchLoan.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    currentSearchQuery = s.toString()
                    applyFilters()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            view.findViewById<ImageButton>(R.id.btnFilterLoans).setOnClickListener { showFilterDialog() }
        } else {
            layoutAdminFilters.visibility = View.GONE
            layoutUserTabs.visibility = View.VISIBLE

            btnActiveLoans.setOnClickListener {
                isHistoryMode = false
                btnActiveLoans.setBackgroundColor(Color.parseColor("#3F51B5"))
                btnHistoryLoans.setBackgroundColor(Color.parseColor("#9E9E9E"))
                loadLoans()
            }

            btnHistoryLoans.setOnClickListener {
                isHistoryMode = true
                btnHistoryLoans.setBackgroundColor(Color.parseColor("#3F51B5"))
                btnActiveLoans.setBackgroundColor(Color.parseColor("#9E9E9E"))
                loadLoans()
            }
        }

        loadLoans()
        return view
    }

    private fun loadLoans() {
        val query = if (isAdmin) {
            firestore.collection("Loans").whereEqualTo("status", "ACTIVE")
        } else {
            val statusToFetch = if (isHistoryMode) "RETURNED" else "ACTIVE"
            firestore.collection("Loans")
                .whereEqualTo("username", currentUsername)
                .whereEqualTo("status", statusToFetch)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val loansList = mutableListOf<Loan>()
                for (doc in documents) {
                    val loan = doc.toObject(Loan::class.java)
                    loan.loanId = doc.id
                    loansList.add(loan)
                }
                currentLoansList = loansList
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Σφάλμα φόρτωσης: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        var result = currentLoansList

        if (currentSearchQuery.isNotEmpty()) {
            result = result.filter { it.bookTitle.contains(currentSearchQuery, ignoreCase = true) }
        }

        if (selectedDeliveryFilter == "Παραλαβή από Κατάστημα") {
            result = result.filter { it.deliveryMethod == "Pickup" }
        } else if (selectedDeliveryFilter == "Αποστολή") {
            result = result.filter { it.deliveryMethod == "Shipping" }
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        result = when (selectedSortIndex) {
            0 -> result.sortedBy { it.bookTitle }
            1 -> result.sortedByDescending { it.bookTitle }
            2 -> result.sortedByDescending { try { sdf.parse(it.borrowDate) } catch (e: Exception) { Date(0) } }
            3 -> result.sortedBy { try { sdf.parse(it.borrowDate) } catch (e: Exception) { Date(0) } }
            else -> result
        }

        loanAdapter.updateLoans(result)

        if (result.isEmpty()) {
            recyclerViewLoans.visibility = View.GONE
            tvEmptyLoans.visibility = View.VISIBLE

            if (!isAdmin && isHistoryMode) {
                tvEmptyLoans.text = "Δεν υπάρχει κανένας παλιός δανεισμός."
            } else {
                tvEmptyLoans.text = "Δεν υπάρχουν ενεργοί δανεισμοί."
            }
        } else {
            recyclerViewLoans.visibility = View.VISIBLE
            tvEmptyLoans.visibility = View.GONE
        }
    }

    private fun showFilterDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_filter_loans, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.show()

        val spinnerSort = dialogView.findViewById<Spinner>(R.id.spinnerLoanSort)
        val sortOptions = arrayOf("Τίτλος (Α-Ω)", "Τίτλος (Ω-Α)", "Ημερομηνία (Νεότερα)", "Ημερομηνία (Παλιότερα)")
        spinnerSort.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort.setSelection(selectedSortIndex)

        val spinnerDelivery = dialogView.findViewById<Spinner>(R.id.spinnerDeliveryFilter)
        val deliveryOptions = arrayOf("Όλοι", "Παραλαβή από Κατάστημα", "Αποστολή")
        spinnerDelivery.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, deliveryOptions)
        spinnerDelivery.setSelection(deliveryOptions.indexOf(selectedDeliveryFilter).takeIf { it >= 0 } ?: 0)

        dialogView.findViewById<Button>(R.id.btnApplyFilters).setOnClickListener {
            selectedSortIndex = spinnerSort.selectedItemPosition
            selectedDeliveryFilter = spinnerDelivery.selectedItem as String
            applyFilters()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnClearFilters).setOnClickListener {
            selectedSortIndex = 0
            selectedDeliveryFilter = "Όλοι"
            applyFilters()
            dialog.dismiss()
        }
    }

    private fun returnBook(loan: Loan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Επιστροφή Βιβλίου")
            .setMessage("Επιβεβαίωση επιστροφής από τον χρήστη ${loan.username};")
            .setPositiveButton("Ναι") { _, _ ->

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayStr = sdf.format(Date())
                val updates = mapOf(
                    "status" to "RETURNED",
                    "returnDate" to todayStr
                )

                firestore.collection("Loans").document(loan.loanId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Επιτυχής επιστροφή!", Toast.LENGTH_SHORT).show()

                        lifecycleScope.launch {
                            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val notifDate = timeFormat.format(Date())

                            db.notificationDao().insertNotification(
                                com.example.mylibrary.data.Notification(
                                    username = loan.username,
                                    message = "Η επιστροφή του βιβλίου '${loan.bookTitle}' ολοκληρώθηκε επιτυχώς!",
                                    date = notifDate
                                )
                            )
                            sendSystemNotification(requireContext(), "Επιστροφή Βιβλίου", "Η επιστροφή του βιβλίου '${loan.bookTitle}' ολοκληρώθηκε επιτυχώς!")
                        }

                        lifecycleScope.launch {
                            try {
                                val books = try {
                                    db.bookDao().getBooksByBranch(loan.branchId).first()
                                } catch (e: Exception) {
                                    db.bookDao().getBooksByBranch(loan.branchId) as List<Book>
                                }
                                val bookToUpdate = books.find { it.title == loan.bookTitle }
                                if (bookToUpdate != null) {
                                    db.bookDao().updateBook(bookToUpdate.copy(stock = bookToUpdate.stock + 1))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        loadLoans()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Αποτυχία επιστροφής.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Ακύρωση", null)
            .show()
    }
}