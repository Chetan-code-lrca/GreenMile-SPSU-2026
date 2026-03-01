package com.spsu.greenmile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.spsu.greenmile.ui.theme.GreenMileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenMileTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var loggedInUser by remember { mutableStateOf("Student") }
                var loggedInRoll by remember { mutableStateOf("") }
                var todayCarbon by remember { mutableStateOf(0.0) }

                when (currentScreen) {
                    "splash" -> SplashScreen(onNavigate = { currentScreen = "login" })
                    "login" -> LoginScreen(onLoginSuccess = { name, roll ->
                        loggedInUser = name
                        loggedInRoll = roll
                        currentScreen = "home"
                    })
                    "home" -> HomeScreen(
                        userName = loggedInUser,
                        onLogActivity = { currentScreen = "log" },
                        onViewLeaderboard = { currentScreen = "leaderboard" }
                    )
                    "log" -> LogActivityScreen(
                        onBack = { currentScreen = "home" },
                        onSubmit = { carbon ->
                            todayCarbon = carbon
                            currentScreen = "home"
                        }
                    )
                    "leaderboard" -> LeaderboardScreen(
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}