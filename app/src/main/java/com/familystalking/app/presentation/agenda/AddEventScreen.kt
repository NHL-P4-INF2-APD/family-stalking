package com.familystalking.app.presentation.agenda

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.navigation.BottomNavBar
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CREATE_EVENT_BUTTON_COLOR = Color(0xFF66BB6A)

@RequiresApi(Build.VERSION_CODES.O) // Make sure this is necessary or handle for lower API levels
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    navController: NavController,
    viewModel: AgendaViewModel = hiltViewModel()
) {
    var eventTitle by remember { mutableStateOf("") }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTime by remember { mutableStateOf(LocalTime(12, 0)) }
    var location by remember { mutableStateOf("") }
    var currentParticipant by remember { mutableStateOf("") }
    val participants = remember { mutableStateListOf<String>() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH)
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Event", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.AddEvent.route,
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Corrected "Event title" label color
            Text(
                "Event title",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Or remove color to use default
            )
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                placeholder = { Text("Avond eten") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent label color
            )
            OutlinedTextField(
                value = selectedDate.toJavaLocalDate().format(dateFormatter),
                onValueChange = { }, // Value changed by dialog
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }, // Field click opens dialog
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Date icon") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) { // Icon click also opens dialog
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Open date picker")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent label color
            )
            OutlinedTextField(
                value = selectedTime.toJavaLocalTime().format(timeFormatter),
                onValueChange = { }, // Value changed by dialog
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }, // Field click opens dialog
                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = "Time icon") },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) { // Icon click also opens dialog
                        Icon(Icons.Outlined.Schedule, contentDescription = "Open time picker")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Location",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent label color
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("Woonkamer") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = "Location icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Participants",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent label color
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = currentParticipant,
                    onValueChange = { currentParticipant = it },
                    placeholder = { Text("Add participants") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Outlined.People, contentDescription = "Participants icon") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (currentParticipant.isNotBlank()) {
                            participants.add(currentParticipant.trim())
                            currentParticipant = ""
                            keyboardController?.hide()
                        }
                    })
                )
                IconButton(onClick = {
                    if (currentParticipant.isNotBlank()) {
                        participants.add(currentParticipant.trim())
                        currentParticipant = ""
                        keyboardController?.hide()
                    }
                }, enabled = currentParticipant.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = "Add participant")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (participants.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    participants.forEach { name ->
                        ParticipantTag( // Ensure ParticipantTag is accessible and accepts modifier
                            name = name,
                            onRemove = { participants.remove(name) },
                            showRemoveIcon = true,
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Button(
                onClick = {
                    if (eventTitle.isNotBlank()) {
                        viewModel.addAgendaEvent(
                            title = eventTitle,
                            date = selectedDate,
                            time = selectedTime,
                            location = location.takeIf { it.isNotBlank() },
                            participants = participants.toList(),
                            description = "New event: $eventTitle" // Or a dedicated description field
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = eventTitle.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CREATE_EVENT_BUTTON_COLOR)
            ) {
                Text("Create event", color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp)) // Bottom padding
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toKotlinLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false // Or use LocalConfiguration.current.is24HourFormat()
        )
        TimePickerDialog( // Ensure TimePickerDialog is defined (likely in AgendaScreen.kt or shared UI)
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        ) {
            TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
        }
    }
}