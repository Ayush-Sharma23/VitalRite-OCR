package com.example.vitalrite_1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vitalrite_1.UserTypeViewModel
import com.example.vitalrite_1.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vitalrite_1.UserTypeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController, title: String) {
    val userPreferences = remember { UserPreferences(navController.context) }
    val viewModel: UserTypeViewModel = viewModel(factory = UserTypeViewModelFactory(userPreferences))
    val userType by userPreferences.userType.collectAsState(initial = null)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val auth = FirebaseAuth.getInstance()
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        navigationIcon = {
            if (currentRoute != "userDashboard" && currentRoute != "doctorDashboard" && currentRoute != "login" && currentRoute != "signup") {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            if (currentRoute != "login" && currentRoute != "signup" && auth.currentUser != null) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Profile", color = Color.Black, fontSize = 16.sp) },
                        onClick = {
                            showMenu = false
                            navController.navigate("editProfile")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Logout", color = Color.Red, fontSize = 16.sp) },
                        onClick = {
                            showMenu = false
                            scope.launch {
                                try {
                                    auth.signOut()
                                    viewModel.clearUserType()
                                    delay(100L) // Slight delay to ensure state is cleared
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Logout", "Failed to sign out: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF6200EA),
            titleContentColor = Color.White
        )
    )
}
