package com.sedexpensetracker.expensetracker.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class TransactionRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val localList = mutableListOf<TransactionModel>()
    private val _localTransactionsFlow = MutableStateFlow<List<TransactionModel>>(emptyList())

    val allTransactions: Flow<List<TransactionModel>> = callbackFlow {
        trySend(localList.toList())

        var firestoreListenerRegistration: ListenerRegistration? = null

        fun attachListeners(uid: String) {
            try {
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = firestore.collection("users")
                    .document(uid)
                    .collection("transactions")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("ExpenseTracker", "Firestore transactions listen offline warning: ${error.localizedMessage}")
                            trySend(localList.toList())
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.toObjects(TransactionModel::class.java)
                            list.sortByDescending { it.dateTimeEpochMillis }
                            localList.clear()
                            localList.addAll(list)
                            _localTransactionsFlow.value = localList.toList()
                            trySend(localList.toList())
                        }
                    }
            } catch (e: Exception) {
                Log.e("ExpenseTracker", "Error attaching transactions listener: ${e.localizedMessage}", e)
                trySend(localList.toList())
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
                trySend(localList.toList())
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListenerRegistration?.remove()
        }
    }

    fun saveTransaction(tx: TransactionModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val user = auth.currentUser
        val uid = user?.uid

        if (tx.id.isBlank()) {
            tx.id = try {
                firestore.collection("users").document().id
            } catch (e: Exception) {
                "tx_${System.currentTimeMillis()}_${(100..999).random()}"
            }
        }

        val existingIndex = localList.indexOfFirst { it.id == tx.id }
        if (existingIndex >= 0) {
            localList[existingIndex] = tx
        } else {
            localList.add(0, tx)
        }
        localList.sortByDescending { it.dateTimeEpochMillis }
        _localTransactionsFlow.value = localList.toList()

        // Invoke completion immediately so local UI updates without waiting on network
        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("transactions")
                    .document(tx.id)
                    .set(tx)
                    .addOnSuccessListener {
                        Log.d("ExpenseTracker", "Firestore transaction write SUCCESS! Document path: users/$uid/transactions/${tx.id}")
                    }
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore transaction write queued offline: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during transaction save: ${e.localizedMessage}")
            }
        }
    }

    fun deleteTransaction(tx: TransactionModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localList.removeAll { it.id == tx.id }
        _localTransactionsFlow.value = localList.toList()
        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank() && tx.id.isNotBlank()) {
            try {
                firestore.collection("users").document(uid).collection("transactions").document(tx.id).delete()
                    .addOnSuccessListener {
                        Log.d("ExpenseTracker", "Firestore transaction delete SUCCESS!")
                    }
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore transaction delete queued offline: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during transaction delete: ${e.localizedMessage}")
            }
        }
    }

    fun clearAll(onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid
        localList.clear()
        _localTransactionsFlow.value = emptyList()
        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank()) {
            try {
                firestore.collection("users").document(uid).collection("transactions").get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()
                        for (doc in snapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("ExpenseTracker", "Firestore clearAll SUCCESS!")
                            }
                            .addOnFailureListener { error ->
                                Log.w("ExpenseTracker", "Firestore clearAll queued offline: ${error.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore clearAll query offline warning: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during clearAll: ${e.localizedMessage}")
            }
        }
    }
}
