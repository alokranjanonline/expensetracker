package com.sedexpensetracker.expensetracker.data.repository

import android.util.Log
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
                        if (error != null) {
                            Log.w("ExpenseTracker", "Firestore budgets listen offline warning: ${error.localizedMessage}")
                            trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.toObjects(BudgetModel::class.java)
                            localBudgets.clear()
                            localBudgets.addAll(list)
                            _localBudgetsFlow.value = localBudgets.toList()
                            trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })
                        }
                    }
            } catch (e: Exception) {
                Log.e("ExpenseTracker", "Error attaching budgets listener: ${e.localizedMessage}", e)
                trySend(localBudgets.filter { it.yearMonth == yearMonth || yearMonth.isBlank() })
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

        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank()) {
            try {
                firestore.collection("users").document(uid).collection("budgets").document(budget.id).set(budget)
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore budget save queued offline: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during budget save: ${e.localizedMessage}")
            }
        }

        return budget.id
    }

    fun deleteBudget(budget: BudgetModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localBudgets.removeAll { it.id == budget.id }
        _localBudgetsFlow.value = localBudgets.toList()

        onComplete?.invoke(true, null)

        if (!uid.isNullOrBlank() && budget.id.isNotBlank()) {
            try {
                firestore.collection("users").document(uid).collection("budgets").document(budget.id).delete()
                    .addOnFailureListener { error ->
                        Log.w("ExpenseTracker", "Firestore budget delete queued offline: ${error.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w("ExpenseTracker", "Offline exception caught during budget delete: ${e.localizedMessage}")
            }
        }
    }
}
