package com.familystalking.app.presentation.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.navigation.BottomNavBar


private val AGENDA_BACKGROUND_COLOR = Color(0xFFF5F5F5)
private val CARD_BACKGROUND_COLOR = Color.White
private val CARD_CORNER_RADIUS = 8.dp
private val GLOBAL_PADDING = 16.dp
private val VERTICAL_ITEM_SPACING = 12.dp

private val PARTICIPANT_TAG_BACKGROUND_COLOR = Color(0xFFE0E0E0)
private val PARTICIPANT_TAG_TEXT_COLOR = Color.DarkGray
private val PARTICIPANT_TAG_SHAPE = RoundedCornerShape(4.dp)
private val PARTICIPANT_TAG_PADDING_VALUES = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

private val FAB_BACKGROUND_COLOR = Color(0xFF66BB6A)
private val FAB_ICON_COLOR = Color.White


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    navController: NavController,
    viewModel: AgendaViewModel = hiltViewModel()
) {
    val agendaItems by viewModel.agendaItems.collectAsState()
    val selectedItem by viewModel.selectedAgendaItem.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEvent.route) }, // Navigate to new screen
                containerColor = FAB_BACKGROUND_COLOR,
                contentColor = FAB_ICON_COLOR,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Outlined.EditCalendar,
                    contentDescription = "Add Agenda Item"
                )
            }
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Agenda.route,
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AGENDA_BACKGROUND_COLOR)
                .padding(paddingValues)
                .padding(horizontal = GLOBAL_PADDING, vertical = VERTICAL_ITEM_SPACING)
        ) {
            if (agendaItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No agenda items yet.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(VERTICAL_ITEM_SPACING)
                ) {
                    items(agendaItems, key = { it.id }) { item ->
                        AgendaItemCard(
                            item = item,
                            onClick = { viewModel.onAgendaItemClick(item) }
                        )
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        AgendaDetailPopup(
            item = item,
            onDismiss = { viewModel.dismissAgendaDetailPopup() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaItemCard(item: AgendaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = CARD_BACKGROUND_COLOR),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(GLOBAL_PADDING)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (item.time.isNotEmpty()) {
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            item.location?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "At: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (item.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.participants.forEach { participant ->
                        ParticipantTag(
                            name = participant,
                            onRemove = null,
                            showRemoveIcon = false,
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaDetailPopup(item: AgendaItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(CARD_CORNER_RADIUS + 4.dp),
            colors = CardDefaults.cardColors(containerColor = CARD_BACKGROUND_COLOR),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(GLOBAL_PADDING * 1.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${item.dateLabel} at ${item.time}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.DarkGray
                )
                item.location?.let {
                    Text(
                        "Location: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                if (item.participants.isNotEmpty()) {
                    Text(
                        text = "Participants:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item.participants.forEach { participant ->
                            ParticipantTag(
                                name = participant,
                                onRemove = null,
                                showRemoveIcon = false,
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Details:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CLOSE")
                }
            }
        }
    }
}

@Composable
fun ParticipantTag(
    name: String,
    onRemove: (() -> Unit)?,
    showRemoveIcon: Boolean,
    modifier: Modifier
) {
    Surface(
        shape = PARTICIPANT_TAG_SHAPE,
        color = PARTICIPANT_TAG_BACKGROUND_COLOR,
        tonalElevation = 1.dp,
        modifier = if (showRemoveIcon) Modifier.clickable(enabled = onRemove != null) { onRemove?.invoke() } else Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PARTICIPANT_TAG_PADDING_VALUES)
        ) {
            Text(
                text = name,
                color = PARTICIPANT_TAG_TEXT_COLOR,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall
            )
            if (showRemoveIcon && onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove $name",
                    modifier = Modifier.size(14.dp),
                    tint = PARTICIPANT_TAG_TEXT_COLOR
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog( // Keep this utility if used by AddEventScreen's date/time pickers
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() } },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}