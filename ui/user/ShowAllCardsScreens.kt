package com.example.vitalrite_1.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timeline
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
import androidx.navigation.NavController
import com.example.vitalrite_1.data.Appointment
import com.example.vitalrite_1.data.Prescription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShowAllCardsScreen(navController: NavController) {
    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var prescriptions by remember { mutableStateOf(listOf<Prescription>()) }
    var appointmentListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var prescriptionListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        appointmentListener = FirebaseFirestore.getInstance()
            .collection("Appointments")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                appointments = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Appointment::class.java)?.copy(id = document.id)
                } ?: emptyList()
            }

        prescriptionListener = FirebaseFirestore.getInstance()
            .collection("Prescriptions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = Date()

                val allPrescriptions = snapshot?.documents?.mapNotNull { doc ->
                    val prescription = doc.toObject(Prescription::class.java)?.copy(id = doc.id)
                    if (prescription != null) {
                        val expiryDate = try {
                            dateFormat.parse(prescription.expiryDate) ?: Date()
                        } catch (e: Exception) {
                            Date()
                        }
                        if (prescription.active && expiryDate.before(currentDate)) {
                            FirebaseFirestore.getInstance().collection("Prescriptions").document(doc.id)
                                .update("active", false)
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

    DisposableEffect(Unit) {
        onDispose {
            appointmentListener?.remove()
            prescriptionListener?.remove()
        }
    }

    val primaryColor = Color(0xFF6200EA)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "All Information",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // First Card: Ongoing Prescriptions
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Second Card: Upcoming Appointments
                item {
                    val upcomingAppointments = appointments.filter { it.isUpcoming() }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Third Card: Medical History Chart
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
            }
        }
    }
}
