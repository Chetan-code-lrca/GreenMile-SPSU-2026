package com.spsu.greenmile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.spsu.greenmile.ui.theme.GreenMileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenMileTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var loggedInUser by remember { mutableStateOf("Student") }
                var loggedInRoll by remember { mutableStateOf("") }
                var loggedInUid by remember { mutableStateOf("") }

                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onNavigate = { currentScreen = "login" }
                    )
                    "login" -> LoginScreen(
                        onLoginSuccess = { name, roll ->
                            loggedInUser = name
                            loggedInRoll = roll
                            loggedInUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
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
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}