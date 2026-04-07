package com.spsu.greenmile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.spsu.greenmile.ui.theme.GreenMileTheme
import com.spsu.greenmile.utils.AuthManager
import com.spsu.greenmile.utils.StreakManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Show status bar and nav bar normally — no edge to edge ──
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            GreenMileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF1F8E9)
                ) {
                    var currentScreen  by remember { mutableStateOf("splash") }
                    var loggedInUser   by remember { mutableStateOf("Student") }
                    var loggedInRoll   by remember { mutableStateOf("") }
                    var loggedInUid    by remember { mutableStateOf("") }
                    var loggedInRole   by remember { mutableStateOf("student") }

                    val db = FirebaseFirestore.getInstance()

                    LaunchedEffect(Unit) {
                        val currentUser = AuthManager.currentUser
                        if (currentUser != null) {
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
                                            StreakManager.checkAndBreakStreakIfMissed(currentUser.uid)
                                            currentScreen = "home"
                                        }
                                        .addOnFailureListener {
                                            currentScreen = "home"
                                        }
                                } else {
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
                                loggedInUid  = AuthManager.currentUser?.uid ?: ""
                                if (loggedInUid.isNotEmpty()) {
                                    db.collection("users").document(loggedInUid).get()
                                        .addOnSuccessListener { doc ->
                                            loggedInRole = doc.getString("role") ?: "student"
                                        }
                                    StreakManager.checkAndBreakStreakIfMissed(loggedInUid)
                                }
                                currentScreen = "home"
                            }
                        )

                        "home" -> HomeScreen(
                            userName       = loggedInUser,
                            userId         = loggedInUid,
                            userRole       = loggedInRole,
                            onLogActivity  = { currentScreen = "log" },
                            onViewLeaderboard = { currentScreen = "leaderboard" },
                            onViewProfile  = { currentScreen = "profile" },
                            onViewHistory  = { currentScreen = "history" },
                            onViewSteps    = { currentScreen = "steps" },
                            onViewAdmin    = { currentScreen = "admin" }
                        )

                        "log" -> LogActivityScreen(
                            userId   = loggedInUid,
                            userName = loggedInUser,
                            userRoll = loggedInRoll,
                            onBack   = { currentScreen = "home" },
                            onSubmit = { currentScreen = "home" }
                        )

                        "leaderboard" -> LeaderboardScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "profile" -> ProfileScreen(
                            userId  = loggedInUid,
                            onBack  = { currentScreen = "home" },
                            onLogout = {
                                AuthManager.logout()
                                loggedInUser = "Student"
                                loggedInRoll = ""
                                loggedInUid  = ""
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