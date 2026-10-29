package com.sedexpensetracker.expensetracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sedexpensetracker.expensetracker.data.models.BudgetModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class BudgetRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val localBudgets = mutableListOf<BudgetModel>()
    private val _localBudgetsFlow = MutableStateFlow<List<BudgetModel>>(emptyList())

    fun getBudgetsForMonth(yearMonth: String): Flow<List<BudgetModel>> = callbackFlow {
        trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })

        var firestoreListenerRegistration: ListenerRegistration? = null

        fun attachListeners(uid: String) {
            try {
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = firestore.collection("users")
                    .document(uid)
                    .collection("budgets")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null) {
                            val list = snapshot.toObjects(BudgetModel::class.java)
                            localBudgets.clear()
                            localBudgets.addAll(list)
                            _localBudgetsFlow.value = localBudgets.toList()
                            trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })
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
                trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListenerRegistration?.remove()
        }
    }

    fun saveBudget(budget: BudgetModel, onComplete: ((Boolean, String?) -> Unit)? = null): String {
        val uid = auth.currentUser?.uid

        if (budget.id.isBlank()) {
            val catKey = budget.categoryId ?: "overall"
            budget.id = "${budget.yearMonth}_$catKey"
        }

        val index = localBudgets.indexOfFirst { it.id == budget.id }
        if (index >= 0) {
            localBudgets[index] = budget
        } else {
            localBudgets.add(budget)
        }
        _localBudgetsFlow.value = localBudgets.toList()

        if (uid != null) {
            firestore.collection("users").document(uid).collection("budgets").document(budget.id).set(budget)
                .addOnSuccessListener { onComplete?.invoke(true, null) }
                .addOnFailureListener { error -> onComplete?.invoke(false, error.localizedMessage) }
        } else {
            onComplete?.invoke(true, null)
        }

        return budget.id
    }

    fun deleteBudget(budget: BudgetModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localBudgets.removeAll { it.id == budget.id }
        _localBudgetsFlow.value = localBudgets.toList()

        if (uid != null && budget.id.isNotBlank()) {
            firestore.collection("users").document(uid).collection("budgets").document(budget.id).delete()
                .addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful, task.exception?.localizedMessage)
                }
        } else {
            onComplete?.invoke(true, null)
        }
    }
}
