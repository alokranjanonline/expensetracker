package com.sedexpensetracker.expensetracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = try {
            firebaseAuth.currentUser
        } catch (e: Exception) {
            null
        }

    val isUserLoggedInAndVerified: Boolean
        get() = try {
            val user = firebaseAuth.currentUser
            user != null && user.isEmailVerified
        } catch (e: Exception) {
            false
        }

    suspend fun registerUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Failed to create user account.")

            // Set User Display Name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("$firstName $lastName".trim())
                .build()
            user.updateProfile(profileUpdates).await()

            // Send Verification Email
            user.sendEmailVerification().await()

            // Sign out immediately so user must verify email before logging in
            firebaseAuth.signOut()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Failed to log in.")

            // Reload user data to get updated email verification status
            user.reload().await()

            if (!user.isEmailVerified) {
                firebaseAuth.signOut()
                Result.failure(Exception("Your account has not been verified. Please check your inbox."))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                user.sendEmailVerification().await()
                firebaseAuth.signOut()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
