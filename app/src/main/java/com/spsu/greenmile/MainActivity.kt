package com.spsu.greenmile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.spsu.greenmile.ui.theme.GreenMileTheme
import com.spsu.greenmile.utils.AuthManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GreenMileTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFFF1F8E9)
                ) {
                    var currentScreen by remember { mutableStateOf("splash") }
                    var loggedInUser by remember { mutableStateOf("Student") }
                    var loggedInRoll by remember { mutableStateOf("") }
                    var loggedInUid by remember { mutableStateOf("") }
                    var loggedInRole by remember { mutableStateOf("student") }

                    val db = FirebaseFirestore.getInstance()

                    // ── On app start: check if user is already logged in + verified ──
                    LaunchedEffect(Unit) {
                        val currentUser = AuthManager.currentUser
                        if (currentUser != null) {
                            // Reload to get latest verification status from Firebase
                            currentUser.reload().addOnSuccessListener {
                                if (currentUser.isEmailVerified) {
                                    loggedInUid = currentUser.uid
                                    db.collection("users").document(currentUser.uid).get()
                                        .addOnSuccessListener { doc ->
                                            if (doc.exists()) {
                                                loggedInUser = doc.getString("name")
                                                    ?: currentUser.email?.substringBefore("@")
                                                            ?: "Student"
                                                loggedInRoll = doc.getString("rollNo") ?: ""
                                                loggedInRole = doc.getString("role") ?: "student"
                                            }
                                            currentScreen = "home"
                                        }
                                        .addOnFailureListener {
                                            currentScreen = "home"
                                        }
                                } else {
                                    // Not verified — force back to login
                                    AuthManager.logout()
                                    currentScreen = "login"
                                }
                            }.addOnFailureListener {
                                currentScreen = "login"
                            }
                        } else {
                            currentScreen = "login"
                        }
                    }

                    when (currentScreen) {
                        "splash" -> SplashScreen(onNavigate = { })

                        "login" -> LoginScreen(
                            onLoginSuccess = { name, roll ->
                                loggedInUser = name
                                loggedInRoll = roll
                                loggedInUid = AuthManager.currentUser?.uid ?: ""

                                // Fetch role from Firestore after login
                                if (loggedInUid.isNotEmpty()) {
                                    db.collection("users").document(loggedInUid).get()
                                        .addOnSuccessListener { doc ->
                                            loggedInRole = doc.getString("role") ?: "student"
                                        }
                                }
                                currentScreen = "home"
                            }
                        )

                        "home" -> HomeScreen(
                            userName = loggedInUser,
                            userId = loggedInUid,
                            userRole = loggedInRole,
                            onLogActivity = { currentScreen = "log" },
                            onViewLeaderboard = { currentScreen = "leaderboard" },
                            onViewProfile = { currentScreen = "profile" },
                            onViewHistory = { currentScreen = "history" },
                            onViewSteps = { currentScreen = "steps" },
                            onViewAdmin = { currentScreen = "admin" }
                        )

                        "log" -> LogActivityScreen(
                            userId = loggedInUid,
                            userName = loggedInUser,
                            userRoll = loggedInRoll,
                            onBack = { currentScreen = "home" },
                            onSubmit = { currentScreen = "home" }
                        )

                        "leaderboard" -> LeaderboardScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "profile" -> ProfileScreen(
                            userId = loggedInUid,
                            onBack = { currentScreen = "home" },
                            onLogout = {
                                AuthManager.logout()
                                loggedInUser = "Student"
                                loggedInRoll = ""
                                loggedInUid = ""
                                loggedInRole = "student"
                                currentScreen = "login"
                            }
                        )

                        "history" -> ActivityHistoryScreen(
                            userId = loggedInUid,
                            onBack = { currentScreen = "home" }
                        )

                        "steps" -> StepCounterScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "admin" -> AdminScreen(
                            userId = loggedInUid,
                            onBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}