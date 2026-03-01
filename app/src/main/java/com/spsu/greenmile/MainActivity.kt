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

                    // Check if user is already logged in
                    LaunchedEffect(Unit) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            // User is already logged in — load their profile
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
                                    // Skip login, go straight to home
                                    currentScreen = "home"
                                }
                                .addOnFailureListener {
                                    // Firestore failed but user is logged in
                                    loggedInUser = currentUser.email
                                        ?.substringBefore("@") ?: "Student"
                                    currentScreen = "home"
                                }
                        } else {
                            // No user logged in — show login
                            currentScreen = "login"
                        }
                    }

                    when (currentScreen) {
                        "splash" -> SplashScreen(
                            onNavigate = { /* handled by LaunchedEffect */ }
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
                            onViewProfile = { currentScreen = "profile" }
                        )
                        "log" -> LogActivityScreen(
                            userId = loggedInUid,
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
                                // Clear all user data on logout
                                loggedInUser = "Student"
                                loggedInRoll = ""
                                loggedInUid = ""
                                currentScreen = "login"
                            }
                        )
                    }
                }
            }
        }
    }
}