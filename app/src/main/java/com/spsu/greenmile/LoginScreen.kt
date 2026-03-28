package com.spsu.greenmile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.spsu.greenmile.utils.AuthManager

@Composable
fun LoginScreen(
    onLoginSuccess: (name: String, roll: String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showVerificationScreen by remember { mutableStateOf(false) }
    var verificationMsg by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableStateOf(false) }

    // ── Forgot Password state ──
    var showForgotDialog by remember { mutableStateOf(false) }

    // ── Forgot Password Dialog ──
    if (showForgotDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotDialog = false }
        )
    }

    if (showVerificationScreen) {
        VerificationWaitingScreen(
            email = email,
            message = verificationMsg,
            resendCooldown = resendCooldown,
            onResend = {
                resendCooldown = true
                AuthManager.resendVerification(
                    onSuccess = {
                        verificationMsg = "✅ Verification email sent to $email"
                        resendCooldown = false
                    },
                    onError = {
                        verificationMsg = "❌ Failed to send email. Try again."
                        resendCooldown = false
                    }
                )
            },
            onCheckVerified = {
                isLoading = true
                AuthManager.refreshAndCheckVerification { isVerified ->
                    isLoading = false
                    if (isVerified) {
                        val uid = AuthManager.currentUser?.uid ?: return@refreshAndCheckVerification
                        AuthManager.getUserData(uid,
                            onSuccess = { n, r, _ -> onLoginSuccess(n, r) },
                            onError = { onLoginSuccess("Student", "") }
                        )
                    } else {
                        verificationMsg = "⚠️ Email not verified yet. Check your inbox."
                    }
                }
            },
            onBack = {
                showVerificationScreen = false
                AuthManager.logout()
            },
            isLoading = isLoading
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "🌱", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "GreenMile", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
        Text(text = "SPSU Campus Sustainability", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // ── Login / SignUp Tab ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Login", "Sign Up").forEachIndexed { index, label ->
                    val selected = (index == 0 && !isSignUp) || (index == 1 && isSignUp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Color(0xFF2E7D32) else Color.White,
                                shape = if (index == 0)
                                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                                else
                                    RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .clickable { isSignUp = index == 1; errorMsg = "" }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else Color.Gray,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Form ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                if (isSignUp) {
                    GreenTextField(value = name, onValueChange = { name = it }, label = "Full Name", placeholder = "e.g. Chetan Sharma")
                    Spacer(modifier = Modifier.height(12.dp))
                    GreenTextField(value = rollNo, onValueChange = { rollNo = it.uppercase() }, label = "Roll Number", placeholder = "e.g. 22BTECH001")
                    Spacer(modifier = Modifier.height(12.dp))
                    GreenTextField(value = department, onValueChange = { department = it }, label = "Department", placeholder = "e.g. Computer Science")
                    Spacer(modifier = Modifier.height(12.dp))
                }

                GreenTextField(
                    value = email, onValueChange = { email = it.trim() },
                    label = "College Email", placeholder = "yourname@spsu.ac.in",
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(12.dp))

                GreenTextField(
                    value = password, onValueChange = { password = it },
                    label = "Password", placeholder = "Minimum 6 characters",
                    isPassword = true, showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )

                if (isSignUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GreenTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = "Confirm Password", placeholder = "Re-enter password",
                        isPassword = true, showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )
                }

                // ── Forgot Password link — only on login tab ──
                if (!isSignUp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showForgotDialog = true }
                            .padding(4.dp)
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(text = errorMsg, color = Color(0xFFB71C1C), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        errorMsg = ""
                        if (isSignUp) {
                            when {
                                name.isBlank()       -> errorMsg = "Please enter your name"
                                rollNo.isBlank()     -> errorMsg = "Please enter your roll number"
                                department.isBlank() -> errorMsg = "Please enter your department"
                                email.isBlank()      -> errorMsg = "Please enter your email"
                                !email.contains("@") -> errorMsg = "Please enter a valid email"
                                password.length < 6  -> errorMsg = "Password must be at least 6 characters"
                                password != confirmPassword -> errorMsg = "Passwords do not match"
                                else -> {
                                    isLoading = true
                                    AuthManager.signUp(
                                        email = email, password = password,
                                        name = name, rollNo = rollNo, department = department,
                                        onSuccess = {
                                            isLoading = false
                                            verificationMsg = "We sent a verification link to $email. Please verify before continuing."
                                            showVerificationScreen = true
                                        },
                                        onError = { msg -> isLoading = false; errorMsg = msg }
                                    )
                                }
                            }
                        } else {
                            when {
                                email.isBlank()    -> errorMsg = "Please enter your email"
                                password.isBlank() -> errorMsg = "Please enter your password"
                                else -> {
                                    isLoading = true
                                    AuthManager.login(
                                        email = email, password = password,
                                        onSuccess = { isVerified ->
                                            isLoading = false
                                            if (isVerified) {
                                                val uid = AuthManager.currentUser?.uid ?: return@login
                                                AuthManager.getUserData(uid,
                                                    onSuccess = { n, r, _ -> onLoginSuccess(n, r) },
                                                    onError = { onLoginSuccess("Student", "") }
                                                )
                                            } else {
                                                verificationMsg = "⚠️ Please verify your email before logging in."
                                                showVerificationScreen = true
                                            }
                                        },
                                        onError = { msg -> isLoading = false; errorMsg = msg }
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (isSignUp) "Create Account" else "Login",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔒", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Use your official @spsu.ac.in email. Admin access is manually approved.", fontSize = 12.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Forgot Password Dialog ──
@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    var resetEmail  by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var successMsg  by remember { mutableStateOf("") }
    var errorMsg    by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🔑", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Reset Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your registered email address. We'll send you a link to reset your password.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value           = resetEmail,
                    onValueChange   = { resetEmail = it.trim(); errorMsg = ""; successMsg = "" },
                    label           = { Text("Email Address") },
                    placeholder     = { Text("yourname@spsu.ac.in", color = Color.LightGray) },
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF2E7D32),
                        focusedLabelColor    = Color(0xFF2E7D32),
                        focusedTextColor     = Color.Black,
                        unfocusedTextColor   = Color.Black
                    )
                )

                // ── Success message ──
                if (successMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text(
                            text     = successMsg,
                            color    = Color(0xFF2E7D32),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── Error message ──
                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text     = errorMsg,
                            color    = Color(0xFFB71C1C),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Send Reset Email button ──
                Button(
                    onClick = {
                        when {
                            resetEmail.isBlank()        -> errorMsg = "Please enter your email"
                            !resetEmail.contains("@")   -> errorMsg = "Please enter a valid email"
                            else -> {
                                isLoading  = true
                                errorMsg   = ""
                                successMsg = ""
                                FirebaseAuth.getInstance()
                                    .sendPasswordResetEmail(resetEmail)
                                    .addOnSuccessListener {
                                        isLoading  = false
                                        successMsg = "✅ Reset link sent to $resetEmail. Check your inbox!"
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        errorMsg  = when {
                                            e.message?.contains("no user record") == true ||
                                                    e.message?.contains("user-not-found") == true ->
                                                "No account found with this email"
                                            e.message?.contains("badly formatted") == true ->
                                                "Invalid email format"
                                            else -> "Failed to send reset email. Try again."
                                        }
                                    }
                            }
                        }
                    },
                    enabled  = !isLoading && successMsg.isEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (successMsg.isEmpty()) "Send Reset Link" else "Link Sent ✅",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { if (!isLoading) onDismiss() }) {
                    Text(text = "← Back to Login", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Verification Screen ──
@Composable
fun VerificationWaitingScreen(
    email: String, message: String, resendCooldown: Boolean,
    onResend: () -> Unit, onCheckVerified: () -> Unit, onBack: () -> Unit, isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📧", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Verify Your Email", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "We sent a verification link to:", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = email, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = if (message.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                )
            ) {
                Text(text = message, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = Color(0xFF1B5E20), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick  = onCheckVerified, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            else Text("✅  I've Verified My Email", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick  = onResend, enabled = !resendCooldown && !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = BorderStroke(1.dp, Color(0xFF2E7D32))
        ) {
            Text(text = if (resendCooldown) "Sending..." else "📨  Resend Verification Email", fontSize = 14.sp, color = Color(0xFF2E7D32))
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(text = "← Back to Login", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// ── Reusable text field ──
@Composable
fun GreenTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = Color.LightGray) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !showPassword)
            PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onTogglePassword != null) {
            { Text(text = if (showPassword) "🙈" else "👁️", modifier = Modifier.clickable { onTogglePassword() }.padding(8.dp), fontSize = 16.sp) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF2E7D32),
            focusedLabelColor    = Color(0xFF2E7D32),
            focusedTextColor     = Color.Black,
            unfocusedTextColor   = Color.Black,
            cursorColor          = Color(0xFF2E7D32),
            focusedPlaceholderColor   = Color(0xFFBDBDBD),
            unfocusedPlaceholderColor = Color(0xFFBDBDBD)
        )
    )
}