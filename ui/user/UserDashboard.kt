package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vitalrite_1.UserTypeViewModel
import com.example.vitalrite_1.UserTypeViewModelFactory
import com.example.vitalrite_1.data.Appointment
import com.example.vitalrite_1.data.Prescription
import com.example.vitalrite_1.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserDashboard(navController: NavController) {
    val userPreferences = remember { UserPreferences(navController.context) }
    val viewModel: UserTypeViewModel = viewModel(factory = UserTypeViewModelFactory(userPreferences))
    val userName by viewModel.userName
    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var prescriptions by remember { mutableStateOf(listOf<Prescription>()) }
    var appointmentListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var prescriptionListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var sosMessage by remember { mutableStateOf("") }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAddPrescriptionDialog by remember { mutableStateOf(false) } // New state for dialog

    // Sample notifications (replace with Firestore data if needed)
    val notifications = listOf(
        "Take Medicine! It is time to take your morning medicines.",
        "Your Appointment is scheduled! Your appointment with Dr. Arya is scheduled for Thursday, 23-08-25 at 15:00",
        "Add Information! Streamline your experience by completing your profile"
    )

    // SOS message auto-dismissal
    LaunchedEffect(sosMessage) {
        if (sosMessage.isNotEmpty()) {
            delay(5000L)
            sosMessage = ""
        }
    }

    // Fetch appointments and prescriptions from Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        appointmentListener = FirebaseFirestore.getInstance()
            .collection("Appointments")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserDashboard", "Listen failed for appointments.", e)
                    return@addSnapshotListener
                }
                appointments = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Appointment::class.java)?.copy(id = document.id)
                } ?: emptyList()
            }

        prescriptionListener = FirebaseFirestore.getInstance()
            .collection("Prescriptions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserDashboard", "Listen failed for prescriptions.", e)
                    return@addSnapshotListener
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = Date()

                val allPrescriptions = snapshot?.documents?.mapNotNull { doc ->
                    val prescription = doc.toObject(Prescription::class.java)?.copy(id = doc.id)
                    if (prescription != null) {
                        val expiryDate = try {
                            dateFormat.parse(prescription.expiryDate) ?: Date()
                        } catch (e: Exception) {
                            Log.e("UserDashboard", "Failed to parse expiryDate for ${prescription.id}: ${e.message}")
                            Date()
                        }

                        if (prescription.active && expiryDate.before(currentDate)) {
                            FirebaseFirestore.getInstance().collection("Prescriptions").document(doc.id)
                                .update("active", false)
                                .addOnSuccessListener {
                                    Log.d("UserDashboard", "Set active=false for prescription ${doc.id} (expired)")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UserDashboard", "Failed to update active field for ${doc.id}: ${e.message}")
                                }
                        }
                        prescription
                    } else {
                        null
                    }
                }?.filterNotNull() ?: emptyList()

                prescriptions = allPrescriptions.filter { prescription ->
                    val expiryDate = try {
                        dateFormat.parse(prescription.expiryDate) ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    prescription.active && expiryDate.after(currentDate)
                }
            }
    }

    // Cleanup listeners on disposal
    DisposableEffect(Unit) {
        onDispose {
            appointmentListener?.remove()
            prescriptionListener?.remove()
        }
    }

    // Define colors and gradients
    val primaryColor = Color(0xFF6200EA)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )
    val sosButtonGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF3D00), Color(0xFFD81B60))
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddPrescriptionDialog = true }) { // Updated to show dialog
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Add Prescription",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { navController.navigate("appointments") }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Check Calendar",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showMoreOptions = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White
                    )
                }
            }
        },
        bottomBar = { UserBottomNav(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sosMessage = "Sending SOS Notification to all Emergency Contact" },
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .background(sosButtonGradient)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Send SOS",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Send SOS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Carousel Hero Section
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // First Card: Ongoing Prescriptions
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .height(120.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (prescriptions.isNotEmpty()) {
                                navController.navigate("prescriptionDetail/${prescriptions.first().id}")
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (prescriptions.isEmpty()) {
                            Text(
                                "No Ongoing Prescriptions",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            )
                        } else {
                            Text(
                                "Ongoing Prescription",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                prescriptions.first().name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Second Card: Upcoming Appointments
                val upcomingAppointments = appointments.filter { it.isUpcoming() }
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .height(120.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (upcomingAppointments.isNotEmpty()) {
                                navController.navigate("appointments")
                            } else {
                                navController.navigate("bookAppointment")
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (upcomingAppointments.isEmpty()) {
                            Text(
                                "No Upcoming Visits",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            )
                        } else {
                            Text(
                                "Upcoming Appointment",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${upcomingAppointments.first().date} at ${upcomingAppointments.first().time}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Third Card: Medical History Chart
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .height(120.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Medical History",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Last Month's Intake",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Chart",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "View Chart",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }
            }

            // Show All Button
            TextButton(
                onClick = { navController.navigate("showAllCards") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    "Show All",
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Notifications Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showNotifications = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Notifications",
                        tint = primaryColor
                    )
                }
            }

            // Sample Notification Previews
            notifications.take(3).forEach { notification ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            notification,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            // SOS Message Display
            AnimatedVisibility(
                visible = sosMessage.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        sosMessage,
                        color = Color.Green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Add Prescription Dialog (New)
    if (showAddPrescriptionDialog) {
        Dialog(onDismissRequest = { showAddPrescriptionDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add Prescription",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TextButton(onClick = {
                        // TODO: Handle Upload Image
                        showAddPrescriptionDialog = false
                    }) {
                        Text("Upload Image", color = Color.Black, fontSize = 16.sp)
                    }
                    TextButton(onClick = {
                        // TODO: Handle Capture Image
                        showAddPrescriptionDialog = false
                    }) {
                        Text("Capture Image", color = Color.Black, fontSize = 16.sp)
                    }
                    TextButton(onClick = {
                        // TODO: Handle Upload PDF
                        showAddPrescriptionDialog = false
                    }) {
                        Text("Upload PDF", color = Color.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // More Options Overlay
    if (showMoreOptions) {
        Dialog(onDismissRequest = { showMoreOptions = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TextButton(onClick = { navController.navigate("account") }) {
                        Text(
                            "Account",
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(onClick = { navController.navigate("help") }) {
                        Text(
                            "Help",
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }) {
                        Text(
                            "Logout",
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Notifications Overlay
    if (showNotifications) {
        Dialog(onDismissRequest = { showNotifications = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(16.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn {
                        items(notifications) { notification ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .shadow(2.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        notification,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
