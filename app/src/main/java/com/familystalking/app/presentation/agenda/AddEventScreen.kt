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
import com.familystalking.app.presentation.family.FamilyMember
import com.familystalking.app.domain.repository.FamilyRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.familystalking.app.presentation.family.FamilyViewModel
import androidx.compose.runtime.getValue

private val CREATE_EVENT_BUTTON_COLOR = Color(0xFF66BB6A)

@RequiresApi(Build.VERSION_CODES.O) // Make sure this is necessary or handle for lower API levels
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    navController: NavController,
    viewModel: AgendaViewModel = hiltViewModel(),
    familyViewModel: FamilyViewModel = hiltViewModel()
) {
    var eventTitle by remember { mutableStateOf("") }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTime by remember { mutableStateOf(LocalTime(12, 0)) }
    var location by remember { mutableStateOf("") }
    val selectedParticipants = remember { mutableStateListOf<String>() } // userIds
    val familyState by familyViewModel.state.collectAsState()
    val friends = familyState.familyMembers

    LaunchedEffect(Unit) {
        if (friends.isEmpty()) {
            familyViewModel.fetchFamilyMembers()
        }
    }

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
                "Deelnemers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (familyState.isLoading) {
                CircularProgressIndicator()
            } else if (friends.isEmpty()) {
                Text("Geen vrienden gevonden", color = Color.Red)
            } else {
                Column {
                    friends.forEach { friend ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedParticipants.contains(friend.id)) {
                                        selectedParticipants.remove(friend.id)
                                    } else {
                                        friend.id?.let { selectedParticipants.add(it) }
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedParticipants.contains(friend.id),
                                onCheckedChange = {
                                    if (it) friend.id?.let { id -> selectedParticipants.add(id) }
                                    else friend.id?.let { id -> selectedParticipants.remove(id) }
                                }
                            )
                            Text(friend.name)
                        }
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
                            participants = selectedParticipants.toList(),
                            description = "New event: $eventTitle"
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