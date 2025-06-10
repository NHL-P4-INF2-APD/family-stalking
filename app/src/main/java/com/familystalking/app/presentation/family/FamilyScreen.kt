package com.familystalking.app.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
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
import com.familystalking.app.domain.repository.PendingRequest
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.navigation.BottomNavBar
import com.familystalking.app.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredMembers = state.familyMembers.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            if (state.pendingRequests.isNotEmpty()) {
                PendingRequestsSection(
                    requests = state.pendingRequests,
                    viewModel = viewModel
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("name...", color = Color.Gray) },
                label = { Text("Search", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "No friends found matching '$searchQuery'"
                        } else {
                            "You haven't added any friends yet."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    filteredMembers.forEach { member ->
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

@Composable
fun PendingRequestsSection(
    requests: List<PendingRequest>,
    viewModel: FamilyViewModel
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Pending Requests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        requests.forEach { request ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${request.senderName} wants to be your friend.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row {
                        Button(
                            onClick = { viewModel.acceptFriendshipRequest(request.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Accept")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.rejectFriendshipRequest(request.id) }) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
} 