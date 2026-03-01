package com.spsu.greenmile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {

    var isLoginMode by remember { mutableStateOf(true) }

    if (isLoginMode) {
        LoginForm(
            onLoginSuccess = onLoginSuccess,
            onSwitchToSignup = { isLoginMode = false }
        )
    } else {
        SignupForm(
            onSignupSuccess = onLoginSuccess,
            onSwitchToLogin = { isLoginMode = true }
        )
    }
}

@Composable
fun LoginForm(
    onLoginSuccess: (String, String) -> Unit,
    onSwitchToSignup: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🌍", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GreenMile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Campus Sustainability Platform",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Welcome Back 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Login with your college email",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("College Email", color = Color(0xFF2E7D32)) },
                        placeholder = { Text("yourname@spsu.ac.in", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color(0xFF2E7D32)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMsg,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMsg = "Please fill all fields"
                                return@Button
                            }
                            if (!email.endsWith("@spsu.ac.in")) {
                                errorMsg = "Please use your SPSU college email (@spsu.ac.in)"
                                return@Button
                            }
                            isLoading = true
                            errorMsg = ""

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            val name = doc.getString("name") ?: "Student"
                                            val roll = doc.getString("rollNo") ?: ""
                                            isLoading = false
                                            onLoginSuccess(name, roll)
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            errorMsg = "Failed to load profile"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMsg = when {
                                        e.message?.contains("password") == true -> "Wrong password"
                                        e.message?.contains("no user") == true -> "No account found. Please sign up."
                                        e.message?.contains("network") == true -> "No internet connection"
                                        else -> "Login failed. Please try again."
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switch to Signup
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to GreenMile? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign Up",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSwitchToSignup() }
                )
            }
        }
    }
}

@Composable
fun SignupForm(
    onSignupSuccess: (String, String) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("CSE") }
    var isStudent by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val departments = listOf("CSE", "ECE", "ME", "Civil", "MBA", "Other")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(text = "🌍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join GreenMile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Create your sustainability profile",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Student/Admin toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDCEDC8), RoundedCornerShape(50))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { isStudent = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStudent) Color(0xFF2E7D32) else Color.Transparent,
                                contentColor = if (isStudent) Color.White else Color.Gray
                            ),
                            elevation = null,
                            shape = RoundedCornerShape(50)
                        ) { Text("Student") }

                        Button(
                            onClick = { isStudent = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isStudent) Color(0xFF2E7D32) else Color.Transparent,
                                contentColor = if (!isStudent) Color.White else Color.Gray
                            ),
                            elevation = null,
                            shape = RoundedCornerShape(50)
                        ) { Text("Admin") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name", color = Color(0xFF2E7D32)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Roll Number
                    OutlinedTextField(
                        value = rollNo,
                        onValueChange = { rollNo = it },
                        label = { Text(if (isStudent) "Roll Number" else "Employee ID", color = Color(0xFF2E7D32)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // College Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("College Email", color = Color(0xFF2E7D32)) },
                        placeholder = { Text("yourname@spsu.ac.in", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)", color = Color(0xFF2E7D32)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFF81C784),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Department
                    Text(
                        text = "Department",
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        departments.take(3).forEach { dept ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (department == dept) Color(0xFF2E7D32) else Color(0xFFF1F8E9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { department = dept }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 13.sp,
                                    color = if (department == dept) Color.White else Color.DarkGray,
                                    fontWeight = if (department == dept) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        departments.drop(3).forEach { dept ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (department == dept) Color(0xFF2E7D32) else Color(0xFFF1F8E9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { department = dept }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 13.sp,
                                    color = if (department == dept) Color.White else Color.DarkGray,
                                    fontWeight = if (department == dept) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMsg,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when {
                                name.isEmpty() || rollNo.isEmpty() || email.isEmpty() || password.isEmpty() ->
                                    errorMsg = "Please fill all fields"
                                !email.endsWith("@spsu.ac.in") ->
                                    errorMsg = "Only SPSU college emails allowed (@spsu.ac.in)"
                                password.length < 6 ->
                                    errorMsg = "Password must be at least 6 characters"
                                else -> {
                                    isLoading = true
                                    errorMsg = ""

                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { result ->
                                            val uid = result.user?.uid ?: ""
                                            val userDoc = mapOf(
                                                "name" to name,
                                                "rollNo" to rollNo,
                                                "email" to email,
                                                "role" to if (isStudent) "student" else "admin",
                                                "department" to department,
                                                "totalPoints" to 0,
                                                "totalCarbonSaved" to 0.0,
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                            db.collection("users").document(uid)
                                                .set(userDoc)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    onSignupSuccess(name, rollNo)
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                    errorMsg = "Account created but profile save failed"
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMsg = when {
                                                e.message?.contains("email") == true -> "Email already registered. Please login."
                                                e.message?.contains("network") == true -> "No internet connection"
                                                else -> "Signup failed. Try again."
                                            }
                                        }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Login",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSwitchToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = "🔒 Only @spsu.ac.in email addresses are allowed to ensure this platform is exclusive to SPSU students and staff.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}