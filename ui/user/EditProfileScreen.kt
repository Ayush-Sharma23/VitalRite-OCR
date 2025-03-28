package com.example.vitalrite_1.ui.user

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.vitalrite_1.R
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

@Composable
fun UserAccountScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    var user by remember { mutableStateOf<User?>(null) }
    var familyMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fetch user data
    LaunchedEffect(Unit) {
        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                user = document.toObject(User::class.java)
            }
            .addOnFailureListener { e ->
                Log.e("UserAccount", "Failed to fetch user: ${e.message}")
            }

        // Fetch family members with same FID
        user?.fid?.takeIf { it.isNotEmpty() }?.let { fid ->
            firestore.collection("Users")
                .whereEqualTo("fid", fid)
                .get()
                .addOnSuccessListener { snapshot ->
                    familyMembers = snapshot.toObjects(User::class.java).filter { it.uid != userId }
                }
                .addOnFailureListener { e ->
                    Log.e("UserAccount", "Failed to fetch family: ${e.message}")
                }
        }
    }

    // Profile picture picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profilePictureUri = it
            val storageRef = storage.reference.child("profile_pictures/$userId")
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        firestore.collection("Users").document(userId)
                            .update("profilePictureUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                user = user?.copy(profilePictureUrl = downloadUri.toString())
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Profile picture updated")
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UserAccount", "Failed to upload picture: ${e.message}")
                }
        }
    }

    val backgroundGradient = Brush.verticalGradient(colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF)))

    Scaffold(
        topBar = {
            TopBar(
                title = "My Account",
                navController = navController,
                actions = {
                    IconButton(onClick = { navController.navigate("calendarScreen") }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendar),
                            contentDescription = "Calendar"
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_more_vert),
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        },
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { ProfileCard(user = u, onProfilePictureClick = { launcher.launch("image/*") }, profilePictureUri = profilePictureUri) }
                    item { HistoryCard(onClick = { navController.navigate("medicalHistoryChartScreen") }) }
                    item { ScheduleCard(user = u) }
                    item { FamilySpaceCard(familyMembers = familyMembers) }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Triple-dot menu overlay
        if (showMenu) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.LightGray)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Info") },
                    onClick = {
                        showMenu = false
                        navController.navigate("editProfileScreen")
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Help") },
                    onClick = {
                        showMenu = false
                        showHelpDialog = true
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Logout", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        showLogoutDialog = true
                    }
                )
            }
        }

        // Help dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Help") },
                text = {
                    Column {
                        Text("Contact us:")
                        Text("Email: support@vitalrite.com")
                        Text("Phone: +1-800-555-1234")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        auth.signOut()
                        navController.navigate("loginScreen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                        showLogoutDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileCard(user: User, onProfilePictureClick: () -> Unit, profilePictureUri: Uri?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = profilePictureUri?.let { rememberAsyncImagePainter(it) }
                    ?: user.profilePictureUrl?.let { rememberAsyncImagePainter(it) }
                    ?: painterResource(id = R.drawable.default_profile),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onProfilePictureClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text("ID: ${user.pid}", fontSize = 14.sp)
                    Text("PHONE: ${user.phone}", fontSize = 14.sp)
                    Text("AGE: ${user.age}", fontSize = 14.sp)
                    Text("BLOOD TYPE: ${user.bloodGroup}", fontSize = 14.sp)
                    Text("SEX: ${user.gender}", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("HISTORY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Placeholder for history data (fetch separately if available)
            Text("Medicines Taken: N/A", fontSize = 14.sp)
            Text("Medicines Missed: N/A", fontSize = 14.sp)
            Text("Doctor Visits: N/A", fontSize = 14.sp)
        }
    }
}

@Composable
fun ScheduleCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("BREAKFAST: ${user.breakfastTime}", fontSize = 14.sp)
            Text("LUNCH: ${user.lunchTime}", fontSize = 14.sp)
            Text("DINNER: ${user.dinnerTime}", fontSize = 14.sp)
            Text("SLEEP: ${user.sleepTime}", fontSize = 14.sp)
        }
    }
}

@Composable
fun FamilySpaceCard(familyMembers: List<User>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.family_icon),
                contentDescription = "Family Icon",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("FAMILY SPACE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                familyMembers.forEachIndexed { index, member ->
                    Text("${index + 1}. ${member.name} (${member.gender}-${member.age})", fontSize = 14.sp)
                }
                if (familyMembers.isEmpty()) {
                    Text("No family members found", fontSize = 14.sp)
                }
            }
        }
    }
}
