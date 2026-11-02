package com.sedexpensetracker.expensetracker

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.sedexpensetracker.expensetracker.notifications.NotificationHelper

class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "FirebaseApp init error: ${e.localizedMessage}")
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
            Log.d("ExpenseTracker", "Firestore persistent offline cache enabled successfully.")
        } catch (e: Exception) {
            Log.w("ExpenseTracker", "Firestore settings configuration warning: ${e.localizedMessage}")
        }

        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Notification channels creation error: ${e.localizedMessage}")
        }
    }
}
