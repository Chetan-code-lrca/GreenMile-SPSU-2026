package com.spsu.greenmile.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object AuthManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ── Current logged in user ──
    val currentUser: FirebaseUser? get() = auth.currentUser

    // ── Sign Up — creates user, saves to Firestore, sends verification email ──
    fun signUp(
        email: String,
        password: String,
        name: String,
        rollNo: String,
        department: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Security: all new users are "student" by default — no one can self-assign admin
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "rollNo" to rollNo,
                    "email" to email,
                    "department" to department,
                    "role" to "student",          // SECURITY: always student on signup
                    "totalPoints" to 0,
                    "totalCarbonSaved" to 0.0,
                    "totalActivitiesLogged" to 0,
                    "currentStreak" to 0,
                    "lastActiveDate" to "",
                    "joinedAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        // Send verification email immediately after signup
                        result.user?.sendEmailVerification()
                            ?.addOnSuccessListener { onSuccess() }
                            ?.addOnFailureListener { onSuccess() } // proceed even if email fails
                    }
                    .addOnFailureListener { onError("Failed to save user data") }
            }
            .addOnFailureListener { e ->
                onError(parseAuthError(e.message ?: "Signup failed"))
            }
    }

    // ── Login — signs in, checks email verification ──
    fun login(
        email: String,
        password: String,
        onSuccess: (isVerified: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                user?.reload()?.addOnSuccessListener {
                    onSuccess(user.isEmailVerified)
                } ?: onSuccess(false)
            }
            .addOnFailureListener { e ->
                onError(parseAuthError(e.message ?: "Login failed"))
            }
    }

    // ── Resend verification email ──
    fun resendVerification(onSuccess: () -> Unit, onError: (String) -> Unit) {
        currentUser?.sendEmailVerification()
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { onError("Failed to send email") }
    }

    // ── Reload user and check if verified ──
    fun refreshAndCheckVerification(onResult: (Boolean) -> Unit) {
        currentUser?.reload()?.addOnSuccessListener {
            onResult(currentUser?.isEmailVerified == true)
        } ?: onResult(false)
    }

    // ── Logout ──
    fun logout() = auth.signOut()

    // ── Get user data from Firestore ──
    fun getUserData(
        uid: String,
        onSuccess: (name: String, roll: String, role: String) -> Unit,
        onError: () -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(
                        doc.getString("name") ?: "",
                        doc.getString("rollNo") ?: "",
                        doc.getString("role") ?: "student"
                    )
                } else onError()
            }
            .addOnFailureListener { onError() }
    }

    // ── Parse Firebase error messages into readable strings ──
    private fun parseAuthError(message: String): String = when {
        message.contains("email address is already in use") -> "This email is already registered"
        message.contains("password is invalid") || message.contains("wrong-password") -> "Incorrect password"
        message.contains("no user record") || message.contains("user-not-found") -> "No account found with this email"
        message.contains("badly formatted") -> "Invalid email format"
        message.contains("weak-password") || message.contains("least 6") -> "Password must be at least 6 characters"
        message.contains("network") -> "No internet connection"
        message.contains("too-many-requests") -> "Too many attempts. Please try again later"
        else -> "Something went wrong. Please try again"
    }
}