package com.capycdr786.todo.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import com.capycdr786.todo.ui.theme.getStatusBadgeColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsSheet(
    task: Task,
    allTasks: List<Task> = emptyList(),
    onStatusChange: (TaskStatus, String?) -> Unit,
    onMoveTask: (LocalDate) -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onDeleteRecurringGroup: ((String) -> Unit)? = null,
    onDeleteRecurringGroupInRange: ((repeatGroupId: String, startDate: LocalDate, endDate: LocalDate) -> Unit)? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var showDatePickerDialog by remember { mutableStateOf(value = false) }
    var pendingStatusChange by remember { mutableStateOf<TaskStatus?>(null) }
    var pendingMoveDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCancellationDialog by remember { mutableStateOf(value = false) }
    var cancellationReasonInput by remember { mutableStateOf("") }
    var showDeleteRecurringTaskDialog by remember { mutableStateOf(value = false) }

    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme() || (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = task.status)
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Due: ${task.date.format(dateFormatter)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Change Status",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskStatus.entries.forEach { taskStatus ->
                    val isSelected = task.status == taskStatus
                    val (containerColor, contentColor) = getStatusBadgeColors(taskStatus, isDarkTheme)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                if (taskStatus == TaskStatus.CANCELLED) {
                                    cancellationReasonInput = task.cancellationReason ?: ""
                                    showCancellationDialog = true
                                } else {
                                    pendingStatusChange = taskStatus
                                }
                            }
                        },
                        label = { Text(taskStatus.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = containerColor,
                            selectedLabelColor = contentColor,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Options",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            TaskOptionItem(
                icon = Icons.Rounded.CalendarMonth,
                title = "Move Task",
                subtitle = "Reschedule to a new date via calendar",
                onClick = {
                    showDatePickerDialog = true
                },
            )

            TaskOptionItem(
                icon = Icons.Rounded.Edit,
                title = "Edit Task",
                subtitle = "Modify title and description",
                onClick = {
                    closeSheet {
                        onEditTask()
                    }
                },
            )

            TaskOptionItem(
                icon = Icons.Rounded.DeleteOutline,
                title = "Delete Task",
                subtitle = "Remove task permanently",
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    closeSheet {
                        onDeleteTask()
                    }
                },
            )

            if (!task.repeatGroupId.isNullOrEmpty()) {
                TaskOptionItem(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Delete Recurring Task",
                    subtitle = "Delete all or a range of recurring instances",
                    iconTint = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeleteRecurringTaskDialog = true
                    },
                )
            }
        }
    }

    if (showDeleteRecurringTaskDialog && !task.repeatGroupId.isNullOrEmpty()) {
        DeleteRecurringTaskDialog(
            task = task,
            allTasks = allTasks,
            onConfirmDeleteAll = { repeatGroupId ->
                showDeleteRecurringTaskDialog = false
                closeSheet {
                    onDeleteRecurringGroup?.invoke(repeatGroupId)
                }
            },
            onConfirmDeleteRange = { repeatGroupId, startDate, endDate ->
                showDeleteRecurringTaskDialog = false
                closeSheet {
                    onDeleteRecurringGroupInRange?.invoke(repeatGroupId, startDate, endDate)
                }
            },
        ) {
            showDeleteRecurringTaskDialog = false
        }
    }

    pendingStatusChange?.let { newStatus ->
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = {
                Text(
                    text = "Change Status",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to change the task status to ${newStatus.displayName}?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingStatusChange = null
                        closeSheet {
                            onStatusChange(newStatus, null)
                        }
                    },
                ) {
                    Text(
                        text = "Confirm",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingStatusChange = null },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showCancellationDialog) {
        AlertDialog(
            onDismissRequest = { showCancellationDialog = false },
            title = {
                Text(
                    text = "Cancel Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column {
                    Text(
                        text = "Reason for cancelling this task:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancellationReasonInput,
                        onValueChange = { cancellationReasonInput = it },
                        placeholder = { Text("Enter reason...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reason = cancellationReasonInput.trim().ifEmpty { null }
                        showCancellationDialog = false
                        closeSheet {
                            onStatusChange(TaskStatus.CANCELLED, reason)
                        }
                    },
                ) {
                    Text(
                        text = "Confirm",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancellationDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDatePickerDialog) {
        val initialEpochMillis = remember(task.date) {
            task.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis,
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            if (pickedDate != task.date) {
                                pendingMoveDate = pickedDate
                            }
                        }
                        showDatePickerDialog = false
                    },
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingMoveDate?.let { selectedDate ->
        AlertDialog(
            onDismissRequest = { pendingMoveDate = null },
            title = {
                Text(
                    text = "Move Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to move this task to ${selectedDate.format(dateFormatter)}?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMoveDate = null
                        closeSheet {
                            onMoveTask(selectedDate)
                        }
                    },
                ) {
                    Text(
                        text = "Confirm",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingMoveDate = null },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TaskOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = textColor,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light Theme", showBackground = true)
@Composable
fun TaskOptionsSheetPreview() {
    MinimalisttodoTheme(darkTheme = false) {
        TaskOptionsSheet(
            task = Task(
                id = "1",
                title = "Design Jetpack Compose UI",
                description = "Build clean M3 task options sheet.",
                status = TaskStatus.CANCELLED,
                cancellationReason = "Requirement changed",
                date = LocalDate.now(),
                repeatGroupId = "group_1",
            ),
            onStatusChange = { _, _ -> },
            onMoveTask = {},
            onEditTask = {},
            onDeleteTask = {},
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaskOptionsSheetDarkPreview() {
    MinimalisttodoTheme(darkTheme = true) {
        TaskOptionsSheet(
            task = Task(
                id = "1",
                title = "Design Jetpack Compose UI",
                description = "Build clean M3 task options sheet.",
                status = TaskStatus.CANCELLED,
                cancellationReason = "Requirement changed",
                date = LocalDate.now(),
                repeatGroupId = "group_1",
            ),
            onStatusChange = { _, _ -> },
            onMoveTask = {},
            onEditTask = {},
            onDeleteTask = {},
            onDismiss = {},
        )
    }
}
