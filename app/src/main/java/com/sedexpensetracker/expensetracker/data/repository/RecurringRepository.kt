package com.sedexpensetracker.expensetracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sedexpensetracker.expensetracker.data.models.RecurringTransactionModel
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar

class RecurringRepository(private val transactionRepository: TransactionRepository = TransactionRepository()) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val localRecurring = mutableListOf<RecurringTransactionModel>()
    private val _localRecurringFlow = MutableStateFlow<List<RecurringTransactionModel>>(emptyList())

    val allRecurringTransactions: Flow<List<RecurringTransactionModel>> = callbackFlow {
        trySend(localRecurring.toList())

        var firestoreListenerRegistration: ListenerRegistration? = null

        fun attachListeners(uid: String) {
            try {
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = firestore.collection("users")
                    .document(uid)
                    .collection("recurring_transactions")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null) {
                            val list = snapshot.toObjects(RecurringTransactionModel::class.java)
                            localRecurring.clear()
                            localRecurring.addAll(list)
                            _localRecurringFlow.value = localRecurring.toList()
                            trySend(localRecurring.toList())
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
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
                trySend(localRecurring.toList())
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListenerRegistration?.remove()
        }
    }

    fun saveRecurringTransaction(item: RecurringTransactionModel, onComplete: ((Boolean, String?) -> Unit)? = null): String {
        val uid = auth.currentUser?.uid

        if (item.id.isBlank()) {
            item.id = firestore.collection("users").document().id
        }

        val index = localRecurring.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            localRecurring[index] = item
        } else {
            localRecurring.add(item)
        }
        _localRecurringFlow.value = localRecurring.toList()

        if (uid != null) {
            firestore.collection("users").document(uid).collection("recurring_transactions").document(item.id).set(item)
                .addOnSuccessListener { onComplete?.invoke(true, null) }
                .addOnFailureListener { error -> onComplete?.invoke(false, error.localizedMessage) }
        } else {
            onComplete?.invoke(true, null)
        }

        return item.id
    }

    fun deleteRecurringTransaction(item: RecurringTransactionModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localRecurring.removeAll { it.id == item.id }
        _localRecurringFlow.value = localRecurring.toList()

        if (uid != null && item.id.isNotBlank()) {
            firestore.collection("users").document(uid).collection("recurring_transactions").document(item.id).delete()
                .addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful, task.exception?.localizedMessage)
                }
        } else {
            onComplete?.invoke(true, null)
        }
    }

    fun processDueRecurringTransactions(currentMillis: Long = System.currentTimeMillis()) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("recurring_transactions").get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        val item = doc.toObject(RecurringTransactionModel::class.java) ?: continue
                        if (item.nextDueDateEpochMillis <= currentMillis) {
                            val tx = TransactionModel(
                                title = item.title,
                                amountMinorUnits = item.amountMinorUnits,
                                type = item.type,
                                categoryId = item.categoryId,
                                categoryName = item.categoryName,
                                categoryIcon = item.categoryIcon,
                                categoryColor = item.categoryColor,
                                dateTimeEpochMillis = item.nextDueDateEpochMillis,
                                notes = item.notes,
                                paymentMethod = item.paymentMethod,
                                isRecurring = true,
                                recurrenceInterval = item.interval
                            )
                            transactionRepository.saveTransaction(tx)

                            val nextDate = calculateNextDueDate(item.nextDueDateEpochMillis, item.interval)
                            item.nextDueDateEpochMillis = nextDate
                            if (item.id.isNotBlank()) {
                                doc.reference.set(item)
                            }
                        }
                    }
                }
        } catch (e: Exception) {}
    }

    private fun calculateNextDueDate(lastDateMillis: Long, interval: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDateMillis
        when (interval.uppercase()) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
