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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spsu.greenmile.ui.theme.GreenMileTheme

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

                    val auth = FirebaseAuth.getInstance()
                    val db = FirebaseFirestore.getInstance()

                    LaunchedEffect(Unit) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            loggedInUid = currentUser.uid
                            db.collection("users").document(currentUser.uid).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        loggedInUser = doc.getString("name")
                                            ?: currentUser.email?.substringBefore("@")
                                                    ?: "Student"
                                        loggedInRoll = doc.getString("rollNo") ?: ""
                                    } else {
                                        loggedInUser = currentUser.email
                                            ?.substringBefore("@") ?: "Student"
                                    }
                                    currentScreen = "home"
                                }
                                .addOnFailureListener {
                                    loggedInUser = currentUser.email
                                        ?.substringBefore("@") ?: "Student"
                                    currentScreen = "home"
                                }
                        } else {
                            currentScreen = "login"
                        }
                    }

                    when (currentScreen) {
                        "splash" -> SplashScreen(
                            onNavigate = { }
                        )
                        "login" -> LoginScreen(
                            onLoginSuccess = { name, roll ->
                                loggedInUser = name
                                loggedInRoll = roll
                                loggedInUid = auth.currentUser?.uid ?: ""
                                currentScreen = "home"
                            }
                        )
                        "home" -> HomeScreen(
                            userName = loggedInUser,
                            userId = loggedInUid,
                            onLogActivity = { currentScreen = "log" },
                            onViewLeaderboard = { currentScreen = "leaderboard" },
                            onViewProfile = { currentScreen = "profile" },
                            onViewHistory = { currentScreen = "history" },
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
                                loggedInUser = "Student"
                                loggedInRoll = ""
                                loggedInUid = ""
                                currentScreen = "login"
                            }
                        )
                        "history" -> ActivityHistoryScreen(
                            userId = loggedInUid,
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