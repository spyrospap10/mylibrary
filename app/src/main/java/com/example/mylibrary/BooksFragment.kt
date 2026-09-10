package com.example.mylibrary

import android.app.AlertDialog
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
import com.example.mylibrary.data.Author
import com.example.mylibrary.data.Branch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BooksFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var bookAdapter: BookAdapter
    private var booksJob: Job? = null
    private var currentBooksList: List<Book> = emptyList()
    private var isAdmin: Boolean = false
    private var authorsMap: Map<Int, String> = emptyMap()
    private var currentSearchQuery = ""
    private var selectedSortIndex = 0
    private var selectedAuthorId = -1
    private var selectedGenre = "Όλα"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_books, container, false)
        db = AppDatabase.getDatabase(requireContext())

        val userRole = requireActivity().intent.getStringExtra("ROLE") ?: "USER"
        isAdmin = (userRole == "ADMIN")

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewBooks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val currentUsername = requireActivity().intent.getStringExtra("USERNAME") ?: "Άγνωστος"
        val firestore = FirebaseFirestore.getInstance()
        bookAdapter = BookAdapter(
            isAdmin = isAdmin,
            onBorrowClick = { book ->
                if (book.stock > 0) {
                    lifecycleScope.launch {
                        firestore.collection("Loans")
                            .whereEqualTo("username", currentUsername)
                            .whereEqualTo("bookTitle", book.title)
                            .whereEqualTo("status", "ACTIVE")
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    Toast.makeText(requireContext(), "Έχετε ήδη δανειστεί αυτό το βιβλίο!", Toast.LENGTH_SHORT).show()
                                } else {
                                    checkBorrowingRights(book, currentUsername, firestore)
                                }
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Δυστυχώς δεν υπάρχει απόθεμα!", Toast.LENGTH_SHORT).show()
                }
            },
            onEditClick = { book -> showEditBookDialog(book) },
            onDeleteClick = { book ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Διαγραφή")
                    .setMessage("Διαγραφή του '${book.title}';")
                    .setPositiveButton("Ναι") { _, _ -> lifecycleScope.launch { db.bookDao().deleteBook(book) } }
                    .setNegativeButton("Ακύρωση", null).show()
            }
        )

        recyclerView.adapter = bookAdapter

        val etSearchBook = view.findViewById<EditText>(R.id.etSearchBook)
        etSearchBook.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<ImageButton>(R.id.btnFilterBooks).setOnClickListener {
            showFilterDialog()
        }

        val spinnerMainBranch = view.findViewById<Spinner>(R.id.spinnerMainBranch)

        lifecycleScope.launch {
            addMassiveSampleDataIfNeeded()
            val authorsList = db.authorDao().getAllAuthors()
            authorsMap = authorsList.associate { it.authorId to "${it.firstName} ${it.lastName}" }

            val branches = db.branchDao().getAllBranches().first()
            if (branches.isNotEmpty()) {
                val branchNames = branches.map { it.name }
                val branchAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, branchNames)
                branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerMainBranch.adapter = branchAdapter

                spinnerMainBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        loadBooksForBranch(branches[position].branchId)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }

        val btnAddBook = view.findViewById<Button>(R.id.btnAddBook)
        if (isAdmin) {
            btnAddBook.visibility = View.VISIBLE
            btnAddBook.setOnClickListener { showAddBookDialog() }
        } else {
            btnAddBook.visibility = View.GONE
        }

        return view
    }

    private fun applyFilters() {
        var result = currentBooksList
        if (currentSearchQuery.isNotEmpty()) {
            result = result.filter { it.title.contains(currentSearchQuery, ignoreCase = true) }
        }
        if (selectedAuthorId != -1) {
            result = result.filter { it.authorId == selectedAuthorId }
        }
        if (selectedGenre != "Όλα") {
            result = result.filter { it.genre == selectedGenre }
        }
        result = when (selectedSortIndex) {
            0 -> result.sortedBy { it.title }
            1 -> result.sortedByDescending { it.title }
            2 -> result.sortedByDescending { it.stock }
            else -> result
        }
        bookAdapter.updateBooks(result, authorsMap)
    }

    private fun showFilterDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_filter_books, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.show()
        val spinnerSort = dialogView.findViewById<Spinner>(R.id.spinnerSort)
        val spinnerAuthor = dialogView.findViewById<Spinner>(R.id.spinnerAuthorFilter)
        val spinnerGenre = dialogView.findViewById<Spinner>(R.id.spinnerGenreFilter)

        val sortOptions = arrayOf("Τίτλος (Α-Ω)", "Τίτλος (Ω-Α)", "Απόθεμα (Φθίνουσα)")
        spinnerSort.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort.setSelection(selectedSortIndex)

        lifecycleScope.launch {
            val allAuthors = db.authorDao().getAllAuthors()
            val authorNames = listOf("Όλοι") + allAuthors.map { "${it.lastName} ${it.firstName}" }
            spinnerAuthor.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, authorNames)

            val currentAuthorIndex = if (selectedAuthorId == -1) 0 else allAuthors.indexOfFirst { it.authorId == selectedAuthorId } + 1
            spinnerAuthor.setSelection(currentAuthorIndex)

            val allGenres = listOf("Όλα") + currentBooksList.map { it.genre }.distinct().sorted()
            spinnerGenre.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allGenres)
            spinnerGenre.setSelection(allGenres.indexOf(selectedGenre).takeIf { it >= 0 } ?: 0)

            dialogView.findViewById<Button>(R.id.btnApplyFilters).setOnClickListener {
                selectedSortIndex = spinnerSort.selectedItemPosition
                selectedAuthorId = if (spinnerAuthor.selectedItemPosition == 0) -1 else allAuthors[spinnerAuthor.selectedItemPosition - 1].authorId
                selectedGenre = spinnerGenre.selectedItem as String
                applyFilters()
                dialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btnClearFilters).setOnClickListener {
                selectedSortIndex = 0
                selectedAuthorId = -1
                selectedGenre = "Όλα"
                applyFilters()
                dialog.dismiss()
            }
        }
    }

    private fun loadBooksForBranch(branchId: Int) {
        booksJob?.cancel()
        booksJob = lifecycleScope.launch {
            try {
                db.bookDao().getBooksByBranch(branchId).collect { books ->
                    currentBooksList = books
                    applyFilters()
                }
            } catch (e: Exception) {
                val books = db.bookDao().getBooksByBranch(branchId).first() as List<Book>
                currentBooksList = books
                applyFilters()
            }
        }
    }

    private suspend fun addMassiveSampleDataIfNeeded() {
        if (db.branchDao().getAllBranches().first().isNotEmpty()) return

        db.branchDao().insertBranch(Branch(name = "Αθήνα", address = "Κέντρο", phone = "2100000000"))
        db.branchDao().insertBranch(Branch(name = "Θεσσαλονίκη", address = "Τσιμισκή", phone = "2310000000"))
        val branches = db.branchDao().getAllBranches().first()
        val athensId = branches[0].branchId
        val thessId = branches[1].branchId

        db.authorDao().insertAuthor(Author(firstName = "J.R.R.", lastName = "Tolkien"))
        db.authorDao().insertAuthor(Author(firstName = "J.K.", lastName = "Rowling"))
        db.authorDao().insertAuthor(Author(firstName = "Agatha", lastName = "Christie"))
        db.authorDao().insertAuthor(Author(firstName = "George", lastName = "Orwell"))
        db.authorDao().insertAuthor(Author(firstName = "Ιούλιος", lastName = "Βερν"))
        val authors = db.authorDao().getAllAuthors()

        val commonBooks = listOf(
            Triple("Ο Άρχοντας των Δαχτυλιδιών", authors[0].authorId, "Φαντασία"),
            Triple("Το Χόμπιτ", authors[0].authorId, "Φαντασία"),
            Triple("Ο Χάρι Πότερ και η Φιλοσοφική Λίθος", authors[1].authorId, "Φαντασία"),
            Triple("Ο Χάρι Πότερ και η Κάμαρα με τα Μυστικά", authors[1].authorId, "Φαντασία"),
            Triple("Έγκλημα στο Οριάν Εξπρές", authors[2].authorId, "Μυστήριο"),
            Triple("Δέκα Μικροί Νέγροι", authors[2].authorId, "Μυστήριο"),
            Triple("1984", authors[3].authorId, "Επιστημονική Φαντασία"),
            Triple("Η Φάρμα των Ζώων", authors[3].authorId, "Κλασική Λογοτεχνία"),
            Triple("Ο Γύρος του Κόσμου σε 80 Ημέρες", authors[4].authorId, "Περιπέτεια"),
            Triple("20.000 Λεύγες Κάτω από τη Θάλασσα", authors[4].authorId, "Περιπέτεια")
        )

        for (b in commonBooks) {
            val year = when(b.first) {
                "1984" -> 1949
                "20.000 Λεύγες Κάτω από τη Θάλασσα" -> 1870
                "Έγκλημα στο Οριάν Εξπρές" -> 1934
                else -> 2000
            }
            db.bookDao().insertBook(Book(title = b.first, publicationYear = year, branchId = athensId, authorId = b.second, stock = (3..10).random(), genre = b.third))
            db.bookDao().insertBook(Book(title = b.first, publicationYear = year, branchId = thessId, authorId = b.second, stock = (3..10).random(), genre = b.third))
        }

        val athensBooks = listOf(
            Triple("Το Σιλμαρίλιον", authors[0].authorId, "Φαντασία"),
            Triple("Ο Χάρι Πότερ και ο Αιχμάλωτος του Αζκαμπάν", authors[1].authorId, "Φαντασία"),
            Triple("Έγκλημα στον Νείλο", authors[2].authorId, "Μυστήριο"),
            Triple("Οι Μέρες της Μπούρμα", authors[3].authorId, "Κλασική Λογοτεχνία"),
            Triple("Ταξίδι στο Κέντρο της Γης", authors[4].authorId, "Περιπέτεια")
        )
        for (b in athensBooks) {
            val year = if (b.first == "Έγκλημα στον Νείλο") 1937 else 2000
            db.bookDao().insertBook(Book(title = b.first, publicationYear = year, branchId = athensId, authorId = b.second, stock = (2..5).random(), genre = b.third))
        }

        val thessBooks = listOf(
            Triple("Τα Παιδιά του Χούριν", authors[0].authorId, "Φαντασία"),
            Triple("Ο Χάρι Πότερ και το Κύπελλο της Φωτιάς", authors[1].authorId, "Φαντασία"),
            Triple("Το Μυστήριο του Μπλε Τρένου", authors[2].authorId, "Μυστήριο"),
            Triple("Φόρος Τιμής στην Καταλωνία", authors[3].authorId, "Ιστορικό"),
            Triple("Από τη Γη στη Σελήνη", authors[4].authorId, "Περιπέτεια")
        )
        for (b in thessBooks) {
            db.bookDao().insertBook(Book(title = b.first, publicationYear = 2000, branchId = thessId, authorId = b.second, stock = (2..5).random(), genre = b.third))
        }
    }

    private fun showAddBookDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_book, null)

        dialogView.findViewById<TextView>(R.id.tvDialogMainTitle)?.text = "Προσθήκη Νέου Βιβλίου"

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.show()

        val etTitle = dialogView.findViewById<EditText>(R.id.etDialogTitle)
        val etYear = dialogView.findViewById<EditText>(R.id.etDialogYear)
        val etStock = dialogView.findViewById<EditText>(R.id.etDialogStock)
        val spinnerGenre = dialogView.findViewById<Spinner>(R.id.spinnerGenre)
        val spinnerAuthor = dialogView.findViewById<Spinner>(R.id.spinnerAuthor)
        val spinnerBranch = dialogView.findViewById<Spinner>(R.id.spinnerBranch)

        val predefinedGenres = arrayOf("Επιστημονική Φαντασία", "Φαντασία", "Μυστήριο", "Περιπέτεια", "Κλασική Λογοτεχνία", "Ιστορικό", "Άλλο")
        spinnerGenre.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, predefinedGenres)

        lifecycleScope.launch {
            val authors = db.authorDao().getAllAuthors()
            val branches = db.branchDao().getAllBranches().first()

            spinnerAuthor.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, authors.map { "${it.lastName} ${it.firstName}" })
            spinnerBranch.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, branches.map { it.name })

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
                if (etTitle.text.isEmpty() || etYear.text.isEmpty() || etStock.text.isEmpty()) {
                    Toast.makeText(context, "Συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    db.bookDao().insertBook(Book(
                        title = etTitle.text.toString().trim(),
                        publicationYear = etYear.text.toString().toInt(),
                        branchId = branches[spinnerBranch.selectedItemPosition].branchId,
                        authorId = authors[spinnerAuthor.selectedItemPosition].authorId,
                        stock = etStock.text.toString().toInt(),
                        genre = spinnerGenre.selectedItem.toString()
                    ))
                    dialog.dismiss()
                    val mainSpinner = requireView().findViewById<Spinner>(R.id.spinnerMainBranch)
                    loadBooksForBranch(branches[mainSpinner.selectedItemPosition].branchId)
                }
            }
        }
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
    }

    private fun showEditBookDialog(bookToEdit: Book) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_book, null)

        dialogView.findViewById<TextView>(R.id.tvDialogMainTitle)?.text = "Επεξεργασία Βιβλίου"

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.show()

        val etTitle = dialogView.findViewById<EditText>(R.id.etDialogTitle)
        val etYear = dialogView.findViewById<EditText>(R.id.etDialogYear)
        val etStock = dialogView.findViewById<EditText>(R.id.etDialogStock)

        val spinnerGenre = dialogView.findViewById<Spinner>(R.id.spinnerGenre)
        val spinnerAuthor = dialogView.findViewById<Spinner>(R.id.spinnerAuthor)
        val spinnerBranch = dialogView.findViewById<Spinner>(R.id.spinnerBranch)

        val predefinedGenres = arrayOf("Επιστημονική Φαντασία", "Φαντασία", "Μυστήριο", "Περιπέτεια", "Κλασική Λογοτεχνία", "Ιστορικό", "Άλλο")
        spinnerGenre.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, predefinedGenres)

        etTitle.setText(bookToEdit.title)
        etYear.setText(bookToEdit.publicationYear.toString())
        etStock.setText(bookToEdit.stock.toString())

        var genreIndex = predefinedGenres.indexOf(bookToEdit.genre)
        if (bookToEdit.genre == "Δυστοπία") {
            genreIndex = 0
        }
        spinnerGenre.setSelection(if (genreIndex >= 0) genreIndex else 0)

        lifecycleScope.launch {
            val authors = db.authorDao().getAllAuthors()
            val branches = db.branchDao().getAllBranches().first()

            spinnerAuthor.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, authors.map { "${it.lastName} ${it.firstName}" })
            spinnerBranch.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, branches.map { it.name })

            spinnerAuthor.setSelection(authors.indexOfFirst { it.authorId == bookToEdit.authorId }.takeIf { it >= 0 } ?: 0)
            spinnerBranch.setSelection(branches.indexOfFirst { it.branchId == bookToEdit.branchId }.takeIf { it >= 0 } ?: 0)

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
                lifecycleScope.launch {
                    db.bookDao().updateBook(bookToEdit.copy(
                        title = etTitle.text.toString().trim(),
                        publicationYear = etYear.text.toString().toInt(),
                        branchId = branches[spinnerBranch.selectedItemPosition].branchId,
                        authorId = authors[spinnerAuthor.selectedItemPosition].authorId,
                        stock = etStock.text.toString().toInt(),
                        genre = spinnerGenre.selectedItem.toString()
                    ))
                    dialog.dismiss()
                    val spinnerMainBranch = requireView().findViewById<Spinner>(R.id.spinnerMainBranch)
                    loadBooksForBranch(branches[spinnerMainBranch.selectedItemPosition].branchId)
                }
            }
        }
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
    }

    private fun executeBorrow(book: Book, username: String, firestore: FirebaseFirestore) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val loanData = hashMapOf(
            "username" to username,
            "bookTitle" to book.title,
            "branchId" to book.branchId,
            "borrowDate" to currentDate,
            "status" to "ACTIVE"
        )

        firestore.collection("Loans")
            .add(loanData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Επιτυχής Δανεισμός!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    val updatedBook = book.copy(stock = book.stock - 1)
                    db.bookDao().updateBook(updatedBook)
                    loadBooksForBranch(book.branchId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Σφάλμα σύνδεσης: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkBorrowingRights(book: Book, username: String, firestore: FirebaseFirestore) {
        lifecycleScope.launch {
            val user = db.userDao().getUserByUsername(username)
            if (!isAdmin && user?.isPremium == false) {
                firestore.collection("Loans")
                    .whereEqualTo("username", username)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.size() >= 1) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Όριο Δανεισμού")
                                .setMessage("Έχετε ήδη 1 ενεργό δανεισμό. Θέλετε να κάνετε συνδρομή για απεριόριστα βιβλία;")
                                .setPositiveButton("Ναι") { _, _ ->
                                    parentFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, SubscriptionsFragment())
                                        .commit()
                                }
                                .setNegativeButton("Όχι", null).show()
                        } else {
                            showAdvancedBorrowDialog(book, username, firestore)
                        }
                    }
            } else {
                showAdvancedBorrowDialog(book, username, firestore)
            }
        }
    }

    private fun showAdvancedBorrowDialog(book: Book, username: String, firestore: FirebaseFirestore) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_borrow_book, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.show()

        val spinnerDuration = dialogView.findViewById<Spinner>(R.id.spinnerDuration)
        val options = arrayOf("1 λεπτό", "7 ημέρες", "14 ημέρες", "30 ημέρες", "60 ημέρες")
        spinnerDuration.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)

        val rgDelivery = dialogView.findViewById<RadioGroup>(R.id.rgDelivery)
        val layoutPickup = dialogView.findViewById<LinearLayout>(R.id.layoutPickup)
        val layoutShipping = dialogView.findViewById<LinearLayout>(R.id.layoutShipping)
        val spinnerPickupBranch = dialogView.findViewById<Spinner>(R.id.spinnerPickupBranch)
        val etPhone = dialogView.findViewById<EditText>(R.id.etUserPhone)
        val etEmail = dialogView.findViewById<EditText>(R.id.etUserEmail)

        lifecycleScope.launch {
            val user = db.userDao().getUserByUsername(username)
            etPhone.setText(user?.phone)
            etEmail.setText(user?.email)
            val branches = db.branchDao().getAllBranches().first().map { it.name }
            spinnerPickupBranch.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, branches)
        }

        rgDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPickup) {
                layoutPickup.visibility = View.VISIBLE
                layoutShipping.visibility = View.GONE
            } else {
                layoutPickup.visibility = View.GONE
                layoutShipping.visibility = View.VISIBLE
            }
        }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            if (rgDelivery.checkedRadioButtonId == -1) {
                Toast.makeText(context, "Επιλέξτε τρόπο παραλαβής!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val calendar = java.util.Calendar.getInstance()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val borrowDate = sdf.format(calendar.time)

            if (spinnerDuration.selectedItemPosition == 0) {
                calendar.add(java.util.Calendar.MINUTE, 1)
            } else {
                val days = when (spinnerDuration.selectedItemPosition) {
                    1 -> 7; 2 -> 14; 3 -> 30; 4 -> 60; else -> 7
                }
                calendar.add(java.util.Calendar.DAY_OF_YEAR, days)
            }
            val returnDate = sdf.format(calendar.time)

            val loanData = hashMapOf(
                "username" to username,
                "bookTitle" to book.title,
                "branchId" to book.branchId,
                "borrowDate" to borrowDate,
                "returnDate" to returnDate,
                "deliveryMethod" to if (rgDelivery.checkedRadioButtonId == R.id.rbPickup) "Pickup" else "Shipping",
                "pickupBranch" to if (rgDelivery.checkedRadioButtonId == R.id.rbPickup) spinnerPickupBranch.selectedItem.toString() else "",
                "address" to dialogView.findViewById<EditText>(R.id.etAddress).text.toString(),
                "city" to dialogView.findViewById<EditText>(R.id.etCity).text.toString(),
                "zip" to dialogView.findViewById<EditText>(R.id.etZip).text.toString(),
                "status" to "ACTIVE"
            )

            firestore.collection("Loans").add(loanData).addOnSuccessListener {
                lifecycleScope.launch {
                    db.bookDao().updateBook(book.copy(stock = book.stock - 1))
                    loadBooksForBranch(book.branchId)
                    dialog.dismiss()
                    Toast.makeText(context, "Ο δανεισμός ολοκληρώθηκε!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
    }
}