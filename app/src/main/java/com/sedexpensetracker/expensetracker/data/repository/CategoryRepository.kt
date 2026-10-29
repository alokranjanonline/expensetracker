package com.sedexpensetracker.expensetracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sedexpensetracker.expensetracker.data.models.CategoryModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class CategoryRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val localCategories = getFreshDefaultCategories().distinctBy { "${it.type.uppercase()}_${it.name.trim().uppercase()}" }.toMutableList()
    private val _localCategoriesFlow = MutableStateFlow<List<CategoryModel>>(localCategories.toList())

    val allCategories: Flow<List<CategoryModel>> = callbackFlow {
        trySend(localCategories.toList())

        var firestoreListenerRegistration: ListenerRegistration? = null

        fun attachListeners(uid: String) {
            try {
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = firestore.collection("users")
                    .document(uid)
                    .collection("categories")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null) {
                            val list = snapshot.toObjects(CategoryModel::class.java)
                            if (list.isEmpty()) {
                                seedDefaultCategories(uid)
                                trySend(getFreshDefaultCategories())
                            } else {
                                // Deduplicate categories by type and name
                                val uniqueList = list.distinctBy { "${it.type.uppercase()}_${it.name.trim().uppercase()}" }.toMutableList()
                                uniqueList.sortBy { it.name }
                                localCategories.clear()
                                localCategories.addAll(uniqueList)
                                _localCategoriesFlow.value = localCategories.toList()
                                trySend(localCategories.toList())
                            }
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
                trySend(localCategories.toList())
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListenerRegistration?.remove()
        }
    }

    private fun seedDefaultCategories(uid: String) {
        val batch = firestore.batch()
        for (category in getFreshDefaultCategories()) {
            val docRef = firestore.collection("users").document(uid).collection("categories").document(category.id)
            batch.set(docRef, category)
        }
        batch.commit()
    }

    fun saveCategory(category: CategoryModel, onComplete: ((Boolean, String?) -> Unit)? = null): String {
        val uid = auth.currentUser?.uid

        if (category.id.isBlank()) {
            category.id = firestore.collection("users").document().id
        }

        // Check if a category with same type and name already exists
        val existingIndex = localCategories.indexOfFirst {
            it.id == category.id || (it.type.equals(category.type, ignoreCase = true) && it.name.trim().equals(category.name.trim(), ignoreCase = true))
        }

        if (existingIndex >= 0) {
            localCategories[existingIndex] = category
        } else {
            localCategories.add(category)
        }
        localCategories.sortBy { it.name }
        _localCategoriesFlow.value = localCategories.toList()

        if (uid != null) {
            firestore.collection("users").document(uid).collection("categories").document(category.id).set(category)
                .addOnSuccessListener { onComplete?.invoke(true, null) }
                .addOnFailureListener { error -> onComplete?.invoke(false, error.localizedMessage) }
        } else {
            onComplete?.invoke(true, null)
        }

        return category.id
    }

    fun deleteCategory(category: CategoryModel, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val uid = auth.currentUser?.uid

        localCategories.removeAll { it.id == category.id }
        _localCategoriesFlow.value = localCategories.toList()

        if (uid != null && category.id.isNotBlank()) {
            firestore.collection("users").document(uid).collection("categories").document(category.id).delete()
                .addOnCompleteListener { task ->
                    onComplete?.invoke(task.isSuccessful, task.exception?.localizedMessage)
                }
        } else {
            onComplete?.invoke(true, null)
        }
    }

    companion object {
        fun getFreshDefaultCategories(): List<CategoryModel> = listOf(
            // Expense Categories
            CategoryModel(id = "cat_1", name = "Food & Dining", type = "EXPENSE", iconName = "Restaurant", colorHex = 0xFFFF5722, isCustom = false),
            CategoryModel(id = "cat_2", name = "Transportation", type = "EXPENSE", iconName = "DirectionsCar", colorHex = 0xFF2196F3, isCustom = false),
            CategoryModel(id = "cat_3", name = "Shopping", type = "EXPENSE", iconName = "ShoppingBag", colorHex = 0xFFE91E63, isCustom = false),
            CategoryModel(id = "cat_4", name = "Bills & Utilities", type = "EXPENSE", iconName = "Receipt", colorHex = 0xFF9C27B0, isCustom = false),
            CategoryModel(id = "cat_5", name = "Entertainment", type = "EXPENSE", iconName = "Movie", colorHex = 0xFF673AB7, isCustom = false),
            CategoryModel(id = "cat_6", name = "Health", type = "EXPENSE", iconName = "LocalHospital", colorHex = 0xFF4CAF50, isCustom = false),
            CategoryModel(id = "cat_7", name = "Education", type = "EXPENSE", iconName = "School", colorHex = 0xFF00BCD4, isCustom = false),
            CategoryModel(id = "cat_8", name = "Rent", type = "EXPENSE", iconName = "Home", colorHex = 0xFF795548, isCustom = false),
            CategoryModel(id = "cat_9", name = "Travel", type = "EXPENSE", iconName = "Flight", colorHex = 0xFF009688, isCustom = false),
            CategoryModel(id = "cat_10", name = "Personal Care", type = "EXPENSE", iconName = "SelfImprovement", colorHex = 0xFFFF9800, isCustom = false),
            CategoryModel(id = "cat_11", name = "Other Expense", type = "EXPENSE", iconName = "MoreHoriz", colorHex = 0xFF607D8B, isCustom = false),

            // Income Categories
            CategoryModel(id = "cat_12", name = "Salary", type = "INCOME", iconName = "Payments", colorHex = 0xFF4CAF50, isCustom = false),
            CategoryModel(id = "cat_13", name = "Freelance", type = "INCOME", iconName = "Work", colorHex = 0xFF00BCD4, isCustom = false),
            CategoryModel(id = "cat_14", name = "Business", type = "INCOME", iconName = "Store", colorHex = 0xFF2196F3, isCustom = false),
            CategoryModel(id = "cat_15", name = "Investments", type = "INCOME", iconName = "TrendingUp", colorHex = 0xFF9C27B0, isCustom = false),
            CategoryModel(id = "cat_16", name = "Gifts", type = "INCOME", iconName = "CardGiftcard", colorHex = 0xFFE91E63, isCustom = false),
            CategoryModel(id = "cat_17", name = "Other Income", type = "INCOME", iconName = "AttachMoney", colorHex = 0xFF8BC34A, isCustom = false)
        )
    }
}
