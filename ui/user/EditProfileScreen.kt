package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var user by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state for editable fields
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var breakfastTime by remember { mutableStateOf("") }
    var lunchTime by remember { mutableStateOf("") }
    var dinnerTime by remember { mutableStateOf("") }
    var sleepTime by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var medicalCondition by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf("") }
    var allergy by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Fetch user data
    LaunchedEffect(Unit) {
        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                user = document.toObject(User::class.java)?.also { u ->
                    name = u.name
                    age = u.age
                    gender = u.gender
                    email = u.email
                    breakfastTime = u.breakfastTime
                    lunchTime = u.lunchTime
                    dinnerTime = u.dinnerTime
                    sleepTime = u.sleepTime
                    bloodGroup = u.bloodGroup
                    medicalCondition = u.medicalCondition
                    operation = u.operation
                    allergy = u.allergy
                    emergencyContact = u.emergencyContact
                    address = u.address
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Failed to fetch user: ${e.message}")
            }
    }

    val backgroundGradient = Brush.verticalGradient(colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF)))

    Scaffold(
        topBar = { TopBar(navController, "Edit Profile") },
        bottomBar = { UserBottomNav(navController) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            user?.let { u ->
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Non-editable fields
                        ReadOnlyField("User ID (UID)", u.uid)
                        ReadOnlyField("Primary ID (PID)", u.pid)
                        ReadOnlyField("Family ID (FID)", u.fid.takeIf { it.isNotEmpty() } ?: "Not Assigned")
                    }
                    item {
                        // Editable fields
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            label = { Text("Gender") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = false // Email typically shouldn't be editable
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = breakfastTime,
                            onValueChange = { breakfastTime = it },
                            label = { Text("Breakfast Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = lunchTime,
                            onValueChange = { lunchTime = it },
                            label = { Text("Lunch Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = dinnerTime,
                            onValueChange = { dinnerTime = it },
                            label = { Text("Dinner Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = sleepTime,
                            onValueChange = { sleepTime = it },
                            label = { Text("Sleep Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true // Corrected typo from "剧单Line" to "singleLine"
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text("Blood Group") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = medicalCondition,
                            onValueChange = { medicalCondition = it },
                            label = { Text("Medical Condition") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = operation,
                            onValueChange = { operation = it },
                            label = { Text("Operation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = allergy,
                            onValueChange = { allergy = it },
                            label = { Text("Allergy") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = emergencyContact,
                            onValueChange = { emergencyContact = it },
                            label = { Text("Emergency Contact") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false
                        )
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updatedUser = u.copy(
                                name = name,
                                age = age,
                                gender = gender,
                                email = email,
                                breakfastTime = breakfastTime,
                                lunchTime = lunchTime,
                                dinnerTime = dinnerTime,
                                sleepTime = sleepTime,
                                bloodGroup = bloodGroup,
                                medicalCondition = medicalCondition,
                                operation = operation,
                                allergy = allergy,
                                emergencyContact = emergencyContact,
                                address = address
                            )
                            firestore.collection("Users").document(userId)
                                .set(updatedUser)
                                .addOnSuccessListener {
                                    Log.d("EditProfile", "Profile updated successfully")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Profile updated successfully")
                                    }
                                    navController.navigate("userDashboard") {
                                        popUpTo("userDashboard") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EditProfile", "Failed to update profile: ${e.message}")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Failed to update profile: ${e.message}")
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Save")
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier
                .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                .padding(8.dp)
                .fillMaxWidth()
        )
    }
}
