package com.example.mylibrary

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.Notification
import com.example.mylibrary.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubscriptionsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUsername = ""
    private var currentUser: User? = null
    private lateinit var tvStatus: TextView
    private lateinit var btnSubscribe: Button
    private lateinit var btnCancelSubscription: Button
    private lateinit var btnStudentDiscount: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)

        tvStatus = view.findViewById(R.id.tvSubscriptionStatus)
        btnSubscribe = view.findViewById(R.id.btnSubscribeNow)
        btnCancelSubscription = view.findViewById(R.id.btnCancelSubscription)
        btnStudentDiscount = view.findViewById(R.id.btnStudentDiscount)

        currentUsername = requireActivity().intent.getStringExtra("USERNAME") ?: ""
        db = AppDatabase.getDatabase(requireContext())

        loadUserStatus()

        btnSubscribe.setOnClickListener { showSubscriptionDialog() }
        btnCancelSubscription.setOnClickListener { cancelActiveSubscription() }
        btnStudentDiscount.setOnClickListener { showStudentLoginDialog() }

        return view
    }

    private fun loadUserStatus() {
        lifecycleScope.launch {
            val users = db.userDao().getAllUsers()
            currentUser = users.find { it.username == currentUsername }

            if (currentUser?.isPremium == true) {
                tvStatus.text = "Ενεργή Συνδρομή!\nΛήγει στις: ${currentUser?.subscriptionEndDate}"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                btnSubscribe.text = "ΑΝΑΝΕΩΣΗ ΣΥΝΔΡΟΜΗΣ"
                btnCancelSubscription.visibility = View.VISIBLE
                btnStudentDiscount.visibility = View.GONE
            } else {
                tvStatus.text = "Δεν έχετε ενεργή συνδρομή."
                tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                btnSubscribe.text = "ΚΑΝΤΕ ΣΥΝΔΡΟΜΗ ΤΩΡΑ"
                btnCancelSubscription.visibility = View.GONE
                btnStudentDiscount.visibility = View.VISIBLE
            }
        }
    }

    private fun showStudentLoginDialog() {
        val context = requireContext()
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        var isVerifying = false

        val dialog = AlertDialog.Builder(context)
            .setTitle("Επαλήθευση Φοιτητή")
            .setView(webView)
            .setNegativeButton("Ακύρωση", null)
            .create()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)

                if (url != null && url.contains("exams-iee.the.ihu.gr") && !url.contains("login")) {
                    if (!isVerifying) {
                        isVerifying = true
                        view?.stopLoading()
                        dialog.dismiss()
                        activateStudentSubscription()
                    }
                }
            }
        }

        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()

        webView.loadUrl("https://sso.ihu.gr/login?service=https%3A%2F%2Fexams-iee.the.ihu.gr%2Flogin%2Findex.php")

        dialog.setOnShowListener {
            webView.requestFocus()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    private fun activateStudentSubscription() {
        val planName = "1 Χρόνος (Φοιτητής)"
        val price = 0.0
        val paymentMethodStr = "Επιβεβαίωση ΔΙΠΑΕ"

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val startDateStr = sdf.format(calendar.time)
        calendar.add(Calendar.YEAR, 1)
        val endDateStr = sdf.format(calendar.time)

        val subscriptionData = hashMapOf(
            "username" to currentUsername,
            "planName" to planName,
            "price" to price,
            "paymentMethod" to paymentMethodStr,
            "startDate" to startDateStr,
            "endDate" to endDateStr,
            "status" to "ACTIVE"
        )

        firestore.collection("Subscriptions")
            .whereEqualTo("username", currentUsername)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener { documents ->
                val batch = firestore.batch()
                for (doc in documents) {
                    batch.update(doc.reference, "status", "EXPIRED")
                }

                batch.commit().addOnCompleteListener {
                    firestore.collection("Subscriptions").add(subscriptionData)
                        .addOnSuccessListener {
                            currentUser?.let { user ->
                                val upgradedUser = user.copy(isPremium = true, subscriptionEndDate = endDateStr)
                                lifecycleScope.launch {
                                    db.userDao().updateUser(upgradedUser)
                                    Toast.makeText(requireContext(), "Επιτυχία! Η φοιτητική σας ιδιότητα επαληθεύτηκε.", Toast.LENGTH_LONG).show()
                                    loadUserStatus()
                                }
                            }
                        }
                }
            }
    }

    private fun cancelActiveSubscription() {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle("Λήξη Συνδρομής")
            .setMessage("Είστε σίγουροι ότι θέλετε να τερματίσετε τη συνδρομή σας τώρα;")
            .setPositiveButton("Ναι, Λήξη") { _, _ ->
                firestore.collection("Subscriptions")
                    .whereEqualTo("username", currentUsername)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .addOnSuccessListener { documents ->
                        val batch = firestore.batch()
                        for (doc in documents) {
                            batch.update(doc.reference, "status", "EXPIRED")
                        }
                        batch.commit().addOnSuccessListener {
                            currentUser?.let { user ->
                                val downgradedUser = user.copy(isPremium = false, subscriptionEndDate = "")
                                lifecycleScope.launch {
                                    db.userDao().updateUser(downgradedUser)

                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val currentDateStr = sdf.format(Date())

                                    db.notificationDao().insertNotification(
                                        Notification(
                                            username = currentUsername,
                                            message = "Η συνδρομή σας έληξε/τερματίστηκε επιτυχώς.",
                                            date = currentDateStr
                                        )
                                    )
                                    sendSystemNotification(requireContext(), "Λήξη Συνδρομής", "Η συνδρομή σας έληξε/τερματίστηκε επιτυχώς.")

                                    Toast.makeText(context, "Η συνδρομή τερματίστηκε.", Toast.LENGTH_SHORT).show()
                                    loadUserStatus()
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Σφάλμα επικοινωνίας. Προσπαθήστε ξανά.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Ακύρωση", null)
            .show()
    }

    private fun showSubscriptionDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_subscribe, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        val rgPlans = dialogView.findViewById<RadioGroup>(R.id.rgPlans)
        val rgPayment = dialogView.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val spinnerStore = dialogView.findViewById<Spinner>(R.id.spinnerStoreSelect)
        val layoutCard = dialogView.findViewById<LinearLayout>(R.id.layoutCardDetails)
        val etCardNum = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etCardExp = dialogView.findViewById<EditText>(R.id.etCardExp)
        val etCardCvv = dialogView.findViewById<EditText>(R.id.etCardCvv)
        val etCardName = dialogView.findViewById<EditText>(R.id.etCardName)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitPayment)

        dialogView.findViewById<RadioButton>(R.id.rbPlanDay)?.text = "1 Λεπτό (Test)"

        etCardExp.addTextChangedListener(object : android.text.TextWatcher {
            var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                var clean = s.toString().replace(Regex("[^\\d]"), "")
                if (clean.length > 2) {
                    clean = clean.substring(0, 2) + "/" + clean.substring(2)
                }
                s.replace(0, s.length, clean)
                isFormatting = false
            }
        })

        val stores = arrayOf("Αθήνα", "Θεσσαλονίκη")
        spinnerStore.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, stores)

        rgPayment.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPayStore -> {
                    spinnerStore.visibility = View.VISIBLE
                    layoutCard.visibility = View.GONE
                }
                R.id.rbPayCard -> {
                    spinnerStore.visibility = View.GONE
                    layoutCard.visibility = View.VISIBLE
                }
            }
        }

        btnSubmit.setOnClickListener {
            val selectedPlanId = rgPlans.checkedRadioButtonId
            val selectedPaymentId = rgPayment.checkedRadioButtonId

            if (selectedPlanId == -1 || selectedPaymentId == -1) {
                Toast.makeText(context, "Παρακαλώ επιλέξτε πακέτο και τρόπο πληρωμής.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var paymentMethodStr = ""
            if (selectedPaymentId == R.id.rbPayStore) {
                paymentMethodStr = "Κατάστημα (${stores[spinnerStore.selectedItemPosition]})"
            } else {
                if (etCardName.text.isEmpty() || etCardNum.text.length != 16 || etCardExp.text.length != 5 || etCardCvv.text.length != 3) {
                    Toast.makeText(context, "Παρακαλώ συμπληρώστε σωστά όλα τα στοιχεία κάρτας.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                paymentMethodStr = "Κάρτα"
            }

            var planName = ""
            var price = 0.0
            val calendar = Calendar.getInstance()

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val startDateStr = sdf.format(calendar.time)

            when (selectedPlanId) {
                R.id.rbPlanDay -> { planName = "1 Λεπτό"; price = 0.0; calendar.add(Calendar.MINUTE, 1) }
                R.id.rbPlanMonth -> { planName = "1 Μήνας"; price = 0.99; calendar.add(Calendar.MONTH, 1) }
                R.id.rbPlanSixMonths -> { planName = "6 Μήνες"; price = 9.99; calendar.add(Calendar.MONTH, 6) }
                R.id.rbPlanYear -> { planName = "1 Χρόνος"; price = 19.99; calendar.add(Calendar.YEAR, 1) }
            }

            val endDateStr = sdf.format(calendar.time)

            val subscriptionData = hashMapOf(
                "username" to currentUsername,
                "planName" to planName,
                "price" to price,
                "paymentMethod" to paymentMethodStr,
                "startDate" to startDateStr,
                "endDate" to endDateStr,
                "status" to "ACTIVE"
            )

            firestore.collection("Subscriptions")
                .whereEqualTo("username", currentUsername)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { documents ->
                    val batch = firestore.batch()
                    for (doc in documents) {
                        batch.update(doc.reference, "status", "EXPIRED")
                    }

                    batch.commit().addOnCompleteListener {
                        firestore.collection("Subscriptions").add(subscriptionData)
                            .addOnSuccessListener {
                                currentUser?.let { user ->
                                    val upgradedUser = user.copy(isPremium = true, subscriptionEndDate = endDateStr)
                                    lifecycleScope.launch {
                                        db.userDao().updateUser(upgradedUser)
                                        dialog.dismiss()
                                        Toast.makeText(context, "Η πληρωμή ολοκληρώθηκε!", Toast.LENGTH_LONG).show()
                                        loadUserStatus()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Σφάλμα κατά την προσθήκη νέας συνδρομής.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Σφάλμα επικοινωνίας με το Firebase.", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }
}