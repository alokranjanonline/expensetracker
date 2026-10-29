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
                            Log.e("ExpenseTracker", "Firestore transactions listen error: ${error.localizedMessage}", error)
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

        Log.d("ExpenseTracker", "saveTransaction called: uid=$uid, title=${tx.title}, amount=${tx.amountMinorUnits}")

        if (tx.id.isBlank()) {
            tx.id = firestore.collection("users").document().id
        }

        val existingIndex = localList.indexOfFirst { it.id == tx.id }
        if (existingIndex >= 0) {
            localList[existingIndex] = tx
        } else {
            localList.add(0, tx)
        }
        localList.sortByDescending { it.dateTimeEpochMillis }
        _localTransactionsFlow.value = localList.toList()

        if (uid.isNullOrBlank()) {
            Log.e("ExpenseTracker", "Cannot save transaction to Firestore: Auth currentUser UID is null!")
            onComplete?.invoke(false, "User is not authenticated.")
            return
        }

        firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .document(tx.id)
            .set(tx)
            .addOnSuccessListener {
                Log.d("ExpenseTracker", "Firestore transaction write SUCCESS! Document path: users/$uid/transactions/${tx.id}")
                onComplete?.invoke(true, null)
            }
            .addOnFailureListener { error ->
                Log.e("ExpenseTracker", "Firestore transaction write FAILED: ${error.localizedMessage}", error)
                onComplete?.invoke(false, error.localizedMessage)
            }
    }

    fun deleteTransaction(tx: TransactionModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localList.removeAll { it.id == tx.id }
        _localTransactionsFlow.value = localList.toList()

        if (!uid.isNullOrBlank() && tx.id.isNotBlank()) {
            firestore.collection("users").document(uid).collection("transactions").document(tx.id).delete()
                .addOnSuccessListener {
                    Log.d("ExpenseTracker", "Firestore transaction delete SUCCESS! Document ID: ${tx.id}")
                    onComplete?.invoke(true, null)
                }
                .addOnFailureListener { error ->
                    Log.e("ExpenseTracker", "Firestore transaction delete FAILED: ${error.localizedMessage}", error)
                    onComplete?.invoke(false, error.localizedMessage)
                }
        } else {
            onComplete?.invoke(true, null)
        }
    }

    fun clearAll(onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid
        localList.clear()
        _localTransactionsFlow.value = emptyList()

        if (!uid.isNullOrBlank()) {
            firestore.collection("users").document(uid).collection("transactions").get()
                .addOnSuccessListener { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("ExpenseTracker", "Firestore clearAll SUCCESS!")
                            onComplete?.invoke(true, null)
                        }
                        .addOnFailureListener { error ->
                            Log.e("ExpenseTracker", "Firestore clearAll FAILED: ${error.localizedMessage}", error)
                            onComplete?.invoke(false, error.localizedMessage)
                        }
                }
                .addOnFailureListener { error ->
                    Log.e("ExpenseTracker", "Firestore clearAll query FAILED: ${error.localizedMessage}", error)
                    onComplete?.invoke(false, error.localizedMessage)
                }
        } else {
            onComplete?.invoke(true, null)
        }
    }
}
