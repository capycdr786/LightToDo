package com.capycdr786.todo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    taskToEdit: Task? = null,
    defaultDate: LocalDate = LocalDate.now(),
    onSave: (title: String, description: String, date: LocalDate, status: TaskStatus, cancellationReason: String?) -> Unit,
    onSaveRepeating: ((title: String, description: String, startDate: LocalDate, endDate: LocalDate, daysOfWeek: Set<DayOfWeek>) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var isTitleError by remember { mutableStateOf(value = false) }

    var isRepeating by remember { mutableStateOf(value = false) }
    var startDate by remember(defaultDate) { mutableStateOf(defaultDate) }
    var endDate by remember(defaultDate) { mutableStateOf(defaultDate.plusDays(7)) }
    var selectedDays by remember {
        mutableStateOf(
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
        )
    }

    var showStartDatePicker by remember { mutableStateOf(value = false) }
    var showEndDatePicker by remember { mutableStateOf(value = false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = taskToEdit != null
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        delay(150.milliseconds)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    fun closeSheet(onComplete: () -> Unit) {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } catch (_: Exception) {
            } finally {
                onComplete()
            }
        }
    }

    fun performSave() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            isTitleError = true
        } else {
            if (!isEditMode && isRepeating && (onSaveRepeating != null)) {
                onSaveRepeating(trimmedTitle, description.trim(), startDate, endDate, selectedDays)
            } else {
                val saveDate = taskToEdit?.date ?: defaultDate
                val saveStatus = taskToEdit?.status ?: TaskStatus.PENDING
                val saveReason = taskToEdit?.cancellationReason
                onSave(trimmedTitle, description.trim(), saveDate, saveStatus, saveReason)
            }
            closeSheet(onDismiss)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (isEditMode) "Edit Task" else "Add New Task",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) isTitleError = false
                },
                label = { Text("Task Title *") },
                placeholder = { Text("What needs to be done?") },
                isError = isTitleError,
                supportingText = if (isTitleError) {
                    { Text("Title cannot be empty") }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("Add additional details...") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        performSave()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            if (!isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Repeat Task",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = isRepeating,
                        onCheckedChange = { isRepeating = it },
                    )
                }

                if (isRepeating) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Start Date", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = startDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("End Date", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = endDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Days of Week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val daysOfWeekList = listOf(
                            DayOfWeek.MONDAY to "Mon",
                            DayOfWeek.TUESDAY to "Tue",
                            DayOfWeek.WEDNESDAY to "Wed",
                            DayOfWeek.THURSDAY to "Thu",
                            DayOfWeek.FRIDAY to "Fri",
                            DayOfWeek.SATURDAY to "Sat",
                            DayOfWeek.SUNDAY to "Sun",
                        )
                        daysOfWeekList.forEach { (day, label) ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - day else selectedDays
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditMode && onDelete != null) {
                    IconButton(
                        onClick = {
                            closeSheet {
                                onDelete()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Task",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { closeSheet(onDismiss) }) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { performSave() },
                    ) {
                        Text(if (isEditMode) "Save" else "Create Task")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val initialMillis = remember(startDate) {
            startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            startDate = picked
                            if (endDate.isBefore(startDate)) {
                                endDate = startDate.plusDays(7)
                            }
                        }
                        showStartDatePicker = false
                    },
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val initialMillis = remember(endDate) {
            endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = if (!picked.isBefore(startDate)) {
                                picked
                            } else {
                                startDate
                            }
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TaskSheetPreview() {
    MinimalisttodoTheme {
        TaskSheet(
            taskToEdit = null,
            onSave = { _, _, _, _, _ -> },
            onDismiss = {},
        )
    }
}
