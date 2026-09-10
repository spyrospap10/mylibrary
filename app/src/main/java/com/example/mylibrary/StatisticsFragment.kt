package com.example.mylibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.AppDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: StatAdapter
    private var allLoans = listOf<DocumentSnapshot>()
    private var allSubs = listOf<DocumentSnapshot>()
    private var booksGenreMap = mapOf<String, String>()
    private var branchesMap = mapOf<Int, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        db = AppDatabase.getDatabase(requireContext())
        val rvStats = view.findViewById<RecyclerView>(R.id.rvStats)
        rvStats.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatAdapter()
        rvStats.adapter = adapter
        val spinner = view.findViewById<Spinner>(R.id.spinnerStats)
        val categories = arrayOf(
            "Βιβλία με τους περισσότερους δανεισμούς",
            "Χρήστες με τους περισσότερους δανεισμούς",
            "Χρήστες με τα περισσότερα έσοδα (Συνδρομές)",
            "Μήνες με τους περισσότερους δανεισμούς",
            "Έσοδα ανά Μήνα",
            "Δημοφιλέστερες Κατηγορίες",
            "Συγκριτικό Υποκαταστημάτων",
            "Τρόπος Παραλαβής"
        )
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        loadAllData(spinner)

        return view
    }

    private fun loadAllData(spinner: Spinner) {
        lifecycleScope.launch {
            val books = db.bookDao().getAllBooks()
            booksGenreMap = books.associate { it.title to it.genre }
            val branches = db.branchDao().getAllBranches().first()
            branchesMap = branches.associate { it.branchId to it.name }
            firestore.collection("Loans").get().addOnSuccessListener { loanSnaps ->
                allLoans = loanSnaps.documents

                firestore.collection("Subscriptions").get().addOnSuccessListener { subSnaps ->
                    allSubs = subSnaps.documents

                    setupSpinnerLogic(spinner)
                }.addOnFailureListener { Toast.makeText(context, "Σφάλμα φόρτωσης", Toast.LENGTH_SHORT).show() }
            }.addOnFailureListener { Toast.makeText(context, "Σφάλμα φόρτωσης", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun setupSpinnerLogic(spinner: Spinner) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val stats = when (position) {
                    0 -> {
                        allLoans.groupingBy { it.getString("bookTitle") ?: "Άγνωστο" }
                            .eachCount().entries.sortedByDescending { it.value }
                            .map { StatItem(it.key, "${it.value} δανεισμοί") }
                    }
                    1 -> {
                        allLoans.groupingBy { it.getString("username") ?: "Άγνωστος" }
                            .eachCount().entries.sortedByDescending { it.value }
                            .map { StatItem(it.key, "${it.value} δανεισμοί") }
                    }
                    2 -> {
                        allSubs.groupBy { it.getString("username") ?: "Άγνωστος" }
                            .map { (user, docs) -> user to docs.sumOf { it.getDouble("price") ?: 0.0 } }
                            .sortedByDescending { it.second }
                            .map { StatItem(it.first, String.format("%.2f €", it.second)) }
                    }
                    3 -> {
                        allLoans.mapNotNull {
                            val date = it.getString("borrowDate") ?: ""
                            if (date.length >= 10) date.substring(3, 10) else null
                        }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                            .map { StatItem(it.key, "${it.value} δανεισμοί") }
                    }
                    4 -> {
                        allSubs.groupBy {
                            val date = it.getString("startDate") ?: ""
                            if (date.length >= 10) date.substring(3, 10) else "Άγνωστος Μήνας"
                        }.map { (month, docs) -> month to docs.sumOf { it.getDouble("price") ?: 0.0 } }
                            .sortedByDescending { it.second }
                            .map { StatItem(it.first, String.format("%.2f €", it.second)) }
                    }
                    5 -> {
                        allLoans.mapNotNull {
                            val title = it.getString("bookTitle") ?: ""
                            booksGenreMap[title] ?: "Άγνωστη"
                        }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                            .map { StatItem(it.key, "${it.value} δανεισμοί") }
                    }
                    6 -> {
                        allLoans.groupBy {
                            val bId = it.getLong("branchId")?.toInt() ?: -1
                            branchesMap[bId] ?: "Άγνωστο"
                        }.map { (branch, docs) -> branch to docs.size }
                            .sortedByDescending { it.second }
                            .map { StatItem(it.first, "${it.second} δανεισμοί") }
                    }
                    7 -> {
                        allLoans.groupingBy {
                            if (it.getString("deliveryMethod") == "Pickup") "Κατάστημα" else "Αποστολή (Courier)"
                        }.eachCount().entries.sortedByDescending { it.value }
                            .map { StatItem(it.key, "${it.value} δανεισμοί") }
                    }
                    else -> emptyList()
                }

                adapter.updateData(stats)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinner.setSelection(0)
    }
}