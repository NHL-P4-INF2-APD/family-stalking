package com.familystalking.app.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.navigation.BottomNavBar
import com.familystalking.app.ui.theme.PrimaryGreen
import android.util.Log // Import Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    Log.e("FAMILY_SCREEN_TEST", "FamilyScreen composable has been CALLED.") // <<< SMOKE TEST LOG
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredMembers = state.familyMembers.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    if (state.showAddFriendDialog && state.scannedUserId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddFriendDialog() },
            title = { Text("Add Friend?") },
            text = { Text("Do you want to send a friend request to ${state.scannedUserName ?: "this user"} (ID: ${state.scannedUserId})?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendFriendshipRequest() },
                    enabled = !state.isSendingFriendRequest
                ) {
                    if (state.isSendingFriendRequest) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Send Request")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddFriendDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.showRequestAlreadyPendingDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRequestAlreadyPendingDialog() },
            title = { Text("Request Status") },
            text = { Text(state.requestAlreadyPendingMessage) },
            confirmButton = {
                Button(onClick = { viewModel.dismissRequestAlreadyPendingDialog() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search family...") },
                    shape = RoundedCornerShape(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { navController.navigate(Screen.PendingRequests.route) }) {
                    BadgedBox(
                        badge = {
                            if (state.pendingRequests.isNotEmpty()) {
                                Badge(
                                    containerColor = PrimaryGreen,
                                    contentColor = Color.White
                                ) { Text("${state.pendingRequests.size}") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Friend Requests",
                            tint = PrimaryGreen
                        )
                    }
                }
            }

            if (state.isLoading && state.familyMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMembers.isEmpty() && state.familyMembers.isNotEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No friends found matching '$searchQuery'",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else if (state.familyMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You haven't added any friends yet. \nScan a QR code or search to add friends.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 8.dp).weight(1f)) {
                    items(filteredMembers) { member ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = member.status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(72.dp)) // Space for FABs and BottomNav
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.FamilyQr.route) },
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 88.dp)
        ) {
            Icon(Icons.Default.QrCode, contentDescription = "Show QR")
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.Camera.route) },
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 88.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Scan QR")
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomNavBar(
                currentRoute = Screen.Family.route,
                navController = navController
            )
        }
    }
}