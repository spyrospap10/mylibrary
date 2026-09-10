package com.example.mylibrary

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.Manifest
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.Notification
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var userRole: String = "USER"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var db: AppDatabase
    private lateinit var navView: NavigationView
    private var loggedInUsername = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        loggedInUsername = intent.getStringExtra("USERNAME") ?: "Άγνωστος"
        userRole = intent.getStringExtra("ROLE") ?: "USER"

        val menu = navView.menu
        val usernameItem = menu.findItem(R.id.nav_logged_in_user)
        usernameItem?.title = "Χρήστης: $loggedInUsername"

        if (userRole == "USER") {
            menu.findItem(R.id.nav_users)?.isVisible = false
            menu.findItem(R.id.nav_info)?.isVisible = false
            menu.findItem(R.id.nav_loans)?.title = "Οι Δανεισμοί μου"
            menu.findItem(R.id.nav_subscriptions)?.title = "Η Συνδρομή μου"

            runBackgroundChecks()
            updateNotificationBadge()

        } else {
            menu.findItem(R.id.nav_notifications)?.isVisible = false
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun runBackgroundChecks() {
        if (userRole == "ADMIN") return

        lifecycleScope.launch {
            val fullSdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val shortSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val now = Date()
            val notifDateStr = fullSdf.format(now)

            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("Loans")
                .whereEqualTo("username", loggedInUsername)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val returnDateStr = doc.getString("returnDate") ?: ""
                        val bookTitle = doc.getString("bookTitle") ?: ""
                        try {
                            val returnDate = try { fullSdf.parse(returnDateStr) } catch (e: Exception) { shortSdf.parse(returnDateStr) }

                            if (returnDate != null && now.after(returnDate)) {
                                val msg = "Το βιβλίο '$bookTitle' έπρεπε να έχει επιστραφεί στις $returnDateStr. Παρακαλούμε να επιστραφεί άμεσα!"

                                lifecycleScope.launch {
                                    val existing = db.notificationDao().getUserNotifications(loggedInUsername).first()
                                    if (!existing.any { it.message == msg }) {
                                        db.notificationDao().insertNotification(
                                            Notification(username = loggedInUsername, message = msg, date = notifDateStr)
                                        )
                                        updateNotificationBadge()
                                        sendSystemNotification(this@MainActivity, "Καθυστέρηση Βιβλίου", msg)
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }


            firestore.collection("Subscriptions")
                .whereEqualTo("username", loggedInUsername)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val endDateStr = doc.getString("endDate") ?: ""
                        try {
                            val endDate = try { fullSdf.parse(endDateStr) } catch (e: Exception) { shortSdf.parse(endDateStr) }

                            if (endDate != null) {
                                if (now.after(endDate)) {
                                    firestore.collection("Subscriptions").document(doc.id).update("status", "EXPIRED")

                                    lifecycleScope.launch {
                                        val user = db.userDao().getUserByUsername(loggedInUsername)
                                        if (user != null) {
                                            db.userDao().updateUser(user.copy(isPremium = false, subscriptionEndDate = ""))
                                        }

                                        val msg = "Η συνδρομή σας έληξε."
                                        val existing = db.notificationDao().getUserNotifications(loggedInUsername).first()

                                        if (!existing.any { it.message == msg && it.date.substring(0, 10) == notifDateStr.substring(0, 10) }) {
                                            db.notificationDao().insertNotification(
                                                Notification(username = loggedInUsername, message = msg, date = notifDateStr)
                                            )
                                            updateNotificationBadge()
                                            sendSystemNotification(this@MainActivity, "Λήξη Συνδρομής", msg)
                                        }
                                    }
                                } else {
                                    val tomorrowCal = Calendar.getInstance()
                                    tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
                                    if (isSameDay(endDate, tomorrowCal.time)) {
                                        val msg = "Η συνδρομή σας λήγει αύριο ($endDateStr)."
                                        lifecycleScope.launch {
                                            val existing = db.notificationDao().getUserNotifications(loggedInUsername).first()
                                            if (!existing.any { it.message == msg }) {
                                                db.notificationDao().insertNotification(
                                                    Notification(username = loggedInUsername, message = msg, date = notifDateStr)
                                                )
                                                updateNotificationBadge()
                                                sendSystemNotification(this@MainActivity, "Λήξη Συνδρομής", msg)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateNotificationBadge() {
        lifecycleScope.launch {
            db.notificationDao().getUnreadCount(loggedInUsername).collect { unreadCount ->
                val badgeTextView = navView.menu.findItem(R.id.nav_notifications).actionView as? TextView

                if (unreadCount > 0) {
                    val bgShape = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 50f
                        setColor(android.graphics.Color.parseColor("#D32F2F"))
                    }

                    badgeTextView?.apply {
                        text = unreadCount.toString()
                        setTextColor(android.graphics.Color.WHITE)
                        background = bgShape
                        setPadding(16, 4, 16, 4)
                        gravity = android.view.Gravity.CENTER
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        textSize = 12f
                    }
                } else {
                    badgeTextView?.text = ""
                    badgeTextView?.background = null
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> openFragment(HomeFragment())
            R.id.nav_books -> openFragment(BooksFragment())
            R.id.nav_loans -> openFragment(LoansFragment())
            R.id.nav_users -> openFragment(UsersFragment())
            R.id.nav_notifications -> openFragment(NotificationsFragment())
            R.id.nav_subscriptions -> {
                if (userRole == "ADMIN") {
                    openFragment(SubscriptionsAdminFragment())
                } else {
                    openFragment(SubscriptionsFragment())
                }
            }
            R.id.nav_info -> openFragment(StatisticsFragment())
            R.id.nav_account -> openFragment(AccountFragment())
            R.id.nav_logout -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Αποσύνδεση")
                    .setMessage("Είστε σίγουροι ότι θέλετε να αποσυνδεθείτε;")
                    .setPositiveButton("Ναι") { _, _ ->
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Όχι", null)
                    .show()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

}

fun sendSystemNotification(context: Context, title: String, message: String) {
    val channelId = "library_notifications"
    val notificationId = 101

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Library Updates"
        val descriptionText = "Ειδοποιήσεις για τη βιβλιοθήκη"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notify(notificationId, builder.build())
        }
    }
}