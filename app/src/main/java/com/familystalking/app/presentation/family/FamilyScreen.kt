package com.familystalking.app.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView // <-- THE FIX
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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

    if (state.showConfirmUnfriendDialog && state.userToUnfriend != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnfriendDialog() },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${state.userToUnfriend?.name} from your friends list?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmUnfriend() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUnfriendDialog() }) {
                    Text("Cancel")
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

            SwipeRefreshList(
                isLoading = state.isLoading,
                onRefresh = { viewModel.fetchFamilyMembers() },
                modifier = Modifier.weight(1f)
            ) {
                if (filteredMembers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No friends found matching '$searchQuery'"
                            } else {
                                "You haven't added any friends yet.\nPull down to refresh."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp).fillMaxSize()) {
                        items(filteredMembers, key = { it.id!! }) { member ->
                            FriendCard(member = member, onUnfriendClick = {
                                viewModel.onUnfriendClick(member)
                            })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(72.dp))
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
fun SwipeRefreshList(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SwipeRefreshLayout(context).apply {
                setOnRefreshListener {
                    onRefresh()
                }

                val composeView = ComposeView(context).apply {
                    setContent {
                        MaterialTheme {
                            content()
                        }
                    }
                }
                addView(composeView)
            }
        },
        update = { swipeRefreshLayout ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
    )
}

@Composable
fun FriendCard(
    member: FamilyMember,
    onUnfriendClick: (FamilyMember) -> Unit
) {
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
                modifier = Modifier.padding(start = 16.dp).weight(1f)
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
            IconButton(onClick = { onUnfriendClick(member) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Friend",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}