package com.sedexpensetracker.expensetracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sedexpensetracker.expensetracker.notifications.NotificationHelper

class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
