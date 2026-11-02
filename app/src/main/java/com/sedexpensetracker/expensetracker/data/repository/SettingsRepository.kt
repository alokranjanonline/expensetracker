package com.sedexpensetracker.expensetracker.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sedexpensetracker.expensetracker.data.models.AppSettingsModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var localSettings = AppSettingsModel()
    private val _localSettingsFlow = MutableStateFlow(localSettings)

    val settings: Flow<AppSettingsModel> = callbackFlow {
        trySend(localSettings)

        var firestoreListenerRegistration: ListenerRegistration? = null

        fun attachListeners(uid: String) {
            try {
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = firestore.collection("users")
                    .document(uid)
                    .collection("settings")
                    .document("app_settings")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("ExpenseTracker", "Firestore settings listen offline warning: ${error.localizedMessage}")
                            trySend(localSettings)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val value = snapshot.toObject(AppSettingsModel::class.java) ?: AppSettingsModel()
                            localSettings = value
                            _localSettingsFlow.value = localSettings
                            trySend(localSettings)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ExpenseTracker", "Error attaching settings listener: ${e.localizedMessage}", e)
                trySend(localSettings)
            }
        }

        val initialUid = auth.currentUser?.uid
        if (initialUid != null) {
            attachListeners(initialUid)
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                attachListeners(uid)
            } else {
                trySend(localSettings)
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListenerRegistration?.remove()
        }
    }

    fun saveSettings(settings: AppSettingsModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localSettings = settings
        _localSettingsFlow.value = localSettings
        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("settings")
                    .document("app_settings")
                    .set(settings)
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore settings save queued offline: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during settings save: ${e.localizedMessage}")
            }
        }
    }
}
