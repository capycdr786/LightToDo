package com.capycdr786.todo.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.ui.components.EmptyTaskState
import com.capycdr786.todo.ui.components.HeaderCalendarBar
import com.capycdr786.todo.ui.components.HorizontalDateSelector
import com.capycdr786.todo.ui.components.TaskItemCard
import com.capycdr786.todo.ui.components.TaskOptionsSheet
import com.capycdr786.todo.ui.components.TaskSheet
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import com.capycdr786.todo.ui.viewmodel.TaskUiState
import com.capycdr786.todo.ui.viewmodel.TaskViewModel
import com.capycdr786.todo.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportData(it, context.contentResolver) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importData(it, context.contentResolver) }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                    )
                }
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDateSelected = { viewModel.setSelectedDate(it) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onSaveTask = { taskToEdit, title, desc, date, status, _ ->
            if (taskToEdit == null) {
                viewModel.addTask(title = title, description = desc, date = date, status = status)
            } else {
                viewModel.updateTask(taskToEdit.copy(title = title, description = desc))
            }
        },
        onSaveRepeatingTask = { title, desc, startDate, endDate, daysOfWeek ->
            viewModel.addRepeatingTask(
                title = title,
                description = desc,
                startDate = startDate,
                endDate = endDate,
                daysOfWeek = daysOfWeek,
            )
        },
        onDeleteRecurringGroup = { repeatGroupId ->
            viewModel.deleteTasksByRepeatGroupId(repeatGroupId)
        },
        onDeleteRecurringGroupInRange = { repeatGroupId, startDate, endDate ->
            viewModel.deleteRepeatGroupTasksInRange(repeatGroupId, startDate, endDate)
        },
        onStatusChange = { task, newStatus, reason ->
            viewModel.setTaskStatus(task.id, newStatus, reason)
        },
        onMoveTask = { task, newDate ->
            viewModel.rescheduleTask(task.id, newDate)
        },
        onBackupData = { exportLauncher.launch("backup.json") },
        onRestoreData = { importLauncher.launch(arrayOf("application/json")) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: TaskUiState,
    snackbarHostState: SnackbarHostState,
    onDateSelected: (LocalDate) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSaveTask: (Task?, String, String, LocalDate, TaskStatus, String?) -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveRepeatingTask: ((String, String, LocalDate, LocalDate, Set<DayOfWeek>) -> Unit)? = null,
    onDeleteRecurringGroup: ((String) -> Unit)? = null,
    onDeleteRecurringGroupInRange: ((repeatGroupId: String, startDate: LocalDate, endDate: LocalDate) -> Unit)? = null,
    onStatusChange: ((Task, TaskStatus, String?) -> Unit)? = null,
    onMoveTask: ((Task, LocalDate) -> Unit)? = null,
) {
    var showAddSheet by remember { mutableStateOf(value = false) }
    var selectedTaskForOptions by remember { mutableStateOf<Task?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            HeaderCalendarBar(
                selectedDate = uiState.selectedDate,
                onDateSelected = onDateSelected,
                taskCountsByDate = uiState.taskCountsByDate,
                onBackupData = onBackupData,
                onRestoreData = onRestoreData,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Task",
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalDateSelector(
                selectedDate = uiState.selectedDate,
                onDateSelected = onDateSelected,
                taskCountsByDate = uiState.taskCountsByDate,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val count = uiState.tasksForSelectedDate.size
                val completedCount = uiState.tasksForSelectedDate.count { it.isCompleted }
                val summaryText = if (count == 0) "0 tasks" else "$completedCount of $count completed"

                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.tasksForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyTaskState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(
                        items = uiState.tasksForSelectedDate,
                        key = { it.id },
                        contentType = { "task" },
                    ) { task ->
                        TaskItemCard(
                            task = task,
                            onOpenOptions = {
                                selectedTaskForOptions = task
                            },
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        TaskSheet(
            defaultDate = uiState.selectedDate,
            onDismiss = { showAddSheet = false },
            onSave = { title, desc, date, status, reason ->
                onSaveTask(null, title, desc, date, status, reason)
            },
            onSaveRepeating = onSaveRepeatingTask,
        )
    }

    selectedTaskForOptions?.let { task ->
        TaskOptionsSheet(
            task = task,
            allTasks = uiState.allTasks,
            onDismiss = { selectedTaskForOptions = null },
            onEditTask = {
                taskToEdit = task
                selectedTaskForOptions = null
            },
            onDeleteTask = {
                taskToDelete = task
                selectedTaskForOptions = null
            },
            onDeleteRecurringGroup = { repeatGroupId ->
                onDeleteRecurringGroup?.invoke(repeatGroupId)
                selectedTaskForOptions = null
            },
            onDeleteRecurringGroupInRange = { repeatGroupId, startDate, endDate ->
                onDeleteRecurringGroupInRange?.invoke(repeatGroupId, startDate, endDate)
                selectedTaskForOptions = null
            },
            onStatusChange = { newStatus, reason ->
                onStatusChange?.invoke(task, newStatus, reason)
            },
            onMoveTask = { newDate ->
                onMoveTask?.invoke(task, newDate)
            },
        )
    }

    taskToEdit?.let { task ->
        TaskSheet(
            taskToEdit = task,
            defaultDate = task.date,
            onDismiss = { taskToEdit = null },
            onSave = { title, desc, date, status, reason ->
                onSaveTask(task, title, desc, date, status, reason)
            },
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete '${task.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTask(task)
                        taskToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MinimalisttodoTheme {
        HomeScreenContent(
            uiState = TaskUiState(
                selectedDate = LocalDate.now(),
                tasksForSelectedDate = listOf(
                    Task(
                        title = "Morning Run",
                        description = "5km around the park",
                        date = LocalDate.now(),
                        status = TaskStatus.DONE,
                        isCompleted = true,
                    ),
                    Task(
                        title = "Design Review",
                        description = "Review new app mocks",
                        date = LocalDate.now(),
                        status = TaskStatus.ONGOING,
                        isCompleted = false,
                    ),
                    Task(
                        title = "Grocery Shopping",
                        description = "",
                        date = LocalDate.now(),
                        status = TaskStatus.PENDING,
                        isCompleted = false,
                    ),
                ),
                taskCountsByDate = mapOf(
                    LocalDate.now() to 3,
                    LocalDate.now().plusDays(1) to 2,
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onDateSelected = {},
            onDeleteTask = {},
            onSaveTask = { _, _, _, _, _, _ -> },
            onBackupData = {},
            onRestoreData = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    MinimalisttodoTheme {
        HomeScreenContent(
            uiState = TaskUiState(
                selectedDate = LocalDate.now(),
                tasksForSelectedDate = listOf(
                    Task(
                        title = "Morning Run",
                        description = "5km around the park",
                        date = LocalDate.now(),
                        status = TaskStatus.DONE,
                        isCompleted = true,
                    ),
                ),
                taskCountsByDate = mapOf(
                    LocalDate.now() to 1,
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onDateSelected = {},
            onDeleteTask = {},
            onSaveTask = { _, _, _, _, _, _ -> },
            onBackupData = {},
            onRestoreData = {},
        )
    }
}
