package com.capycdr786.todo.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.data.repository.TaskRepository
import com.capycdr786.todo.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
    ) : UiEvent()
}

data class TaskUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val taskCountsByDate: Map<LocalDate, Int> = emptyMap(),
    val totalTaskCount: Int = allTasks.size,
    val isLoading: Boolean = false,
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val taskCountsByDate: StateFlow<Map<LocalDate, Int>> = repository.getTaskCountsByDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<TaskUiState> = combine(
        _selectedDate,
        tasksForSelectedDate,
        allTasks,
        taskCountsByDate,
    ) { date, tasksForDate, all, taskCounts ->
        TaskUiState(
            selectedDate = date,
            tasksForSelectedDate = tasksForDate,
            allTasks = all,
            taskCountsByDate = taskCounts,
            totalTaskCount = all.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState(),
    )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addTask(
        title: String,
        description: String = "",
        date: LocalDate = _selectedDate.value,
        @Suppress("UNUSED_PARAMETER") status: TaskStatus = TaskStatus.PENDING,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        _selectedDate.value = date
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.addTask(
                    Task(
                        title = trimmedTitle,
                        description = description.trim(),
                        date = date,
                        status = TaskStatus.PENDING,
                        isCompleted = false,
                    ),
                )
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to add task"))
            }
        }
    }

    @Suppress("unused")
    fun addTask(task: Task) {
        if (task.title.isBlank()) return
        _selectedDate.value = task.date
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.addTask(task.copy(status = TaskStatus.PENDING, isCompleted = false))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to add task"))
            }
        }
    }

    fun addRepeatingTask(
        title: String,
        description: String = "",
        startDate: LocalDate,
        endDate: LocalDate,
        daysOfWeek: Set<DayOfWeek>,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank() || daysOfWeek.isEmpty() || startDate.isAfter(endDate)) return
        _selectedDate.value = startDate
        viewModelScope.launch(ioDispatcher) {
            try {
                val repeatGroupId = UUID.randomUUID().toString()
                val tasks = mutableListOf<Task>()
                var currDate = startDate
                while (!currDate.isAfter(endDate)) {
                    if (daysOfWeek.contains(currDate.dayOfWeek)) {
                        tasks.add(
                            Task(
                                title = trimmedTitle,
                                description = description.trim(),
                                date = currDate,
                                status = TaskStatus.PENDING,
                                isCompleted = false,
                                repeatGroupId = repeatGroupId,
                            ),
                        )
                    }
                    currDate = currDate.plusDays(1)
                }
                if (tasks.isNotEmpty()) {
                    repository.insertTasks(tasks)
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding repeating tasks: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to add repeating tasks"))
            }
        }
    }

    fun updateTask(task: Task) {
        _selectedDate.value = task.date
        viewModelScope.launch(ioDispatcher) {
            try {
                val existingTask = repository.getTaskById(task.id)
                val dateChanged = (existingTask != null) && (existingTask.date != task.date)
                val updatedStatus = if (dateChanged) TaskStatus.PENDING else task.status
                val isCompleted = !dateChanged && ((updatedStatus == TaskStatus.DONE) || task.isCompleted)
                val reason = if (updatedStatus == TaskStatus.CANCELLED) task.cancellationReason else null
                repository.updateTask(task.copy(status = updatedStatus, isCompleted = isCompleted, cancellationReason = reason))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to update task"))
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting task: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to delete task"))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteTaskById(taskId)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting task by ID: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to delete task"))
            }
        }
    }

    fun deleteTasksInRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val count = repository.deleteTasksInRange(startDate, endDate)
                _uiEvent.send(UiEvent.ShowSnackbar("$count tasks deleted"))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting tasks in range: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to delete tasks"))
            }
        }
    }

    fun deleteRepeatGroupTasks(repeatGroupId: String) {
        deleteTasksByRepeatGroupId(repeatGroupId)
    }

    fun deleteTasksByRepeatGroupId(repeatGroupId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val count = repository.deleteTasksByRepeatGroupId(repeatGroupId)
                _uiEvent.send(UiEvent.ShowSnackbar("$count recurring tasks deleted"))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting tasks by repeat group: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to delete recurring tasks"))
            }
        }
    }

    fun deleteRepeatGroupTasksInRange(repeatGroupId: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val count = repository.deleteRepeatGroupTasksInRange(repeatGroupId, startDate, endDate)
                _uiEvent.send(UiEvent.ShowSnackbar("$count recurring tasks deleted"))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting repeating tasks in range: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to delete recurring tasks"))
            }
        }
    }

    @Suppress("unused")
    fun toggleTaskCompletion(task: Task) {
        toggleTaskCompletion(task.id)
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.toggleTaskCompletion(taskId)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error toggling task completion: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to update task"))
            }
        }
    }

    @Suppress("unused")
    fun rescheduleTask(task: Task, newDate: LocalDate) {
        rescheduleTask(task.id, newDate)
    }

    fun rescheduleTask(taskId: String, newDate: LocalDate) {
        _selectedDate.value = newDate
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.rescheduleTask(taskId, newDate)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error rescheduling task: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to reschedule task"))
            }
        }
    }

    @Suppress("unused")
    fun setTaskStatus(task: Task, newStatus: TaskStatus, cancellationReason: String? = null) {
        setTaskStatus(task.id, newStatus, cancellationReason)
    }

    fun setTaskStatus(taskId: String, newStatus: TaskStatus, cancellationReason: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.setTaskStatus(taskId, newStatus, cancellationReason)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error setting task status: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to update task status"))
            }
        }
    }

    fun exportData(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val tasks = repository.getAllTasks().first()
                val json = Json { prettyPrint = true }
                val jsonString = json.encodeToString(tasks)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                _uiEvent.send(UiEvent.ShowSnackbar("Data exported successfully"))
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error exporting data: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to export data"))
            }
        }
    }

    fun importData(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val maxSizeBytes = 5 * 1024 * 1024
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    val outputStream = ByteArrayOutputStream()
                    var totalBytes = 0
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        if (totalBytes > maxSizeBytes) {
                            throw IllegalArgumentException("Import file exceeds 5MB limit")
                        }
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.toString(Charsets.UTF_8.name())
                }

                if (!jsonString.isNullOrBlank()) {
                    val json = Json { ignoreUnknownKeys = true }
                    val tasks = json.decodeFromString<List<Task>>(jsonString)
                    val validTasks = tasks.asSequence()
                        .filter { it.title.isNotBlank() }
                        .map { task ->
                            task.copy(
                                id = task.id.ifBlank { UUID.randomUUID().toString() },
                                title = task.title.trim(),
                                description = task.description.trim(),
                            )
                        }
                        .toList()

                    if (validTasks.isNotEmpty()) {
                        repository.insertTasks(validTasks)
                        _uiEvent.send(UiEvent.ShowSnackbar("Successfully imported ${validTasks.size} tasks"))
                    } else {
                        _uiEvent.send(UiEvent.ShowSnackbar("No valid tasks found in import file"))
                    }
                } else {
                    _uiEvent.send(UiEvent.ShowSnackbar("Selected file was empty"))
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error importing data: ${e.message}", e)
                _uiEvent.send(UiEvent.ShowSnackbar("Failed to import data: ${e.localizedMessage ?: "Invalid file"}"))
            }
        }
    }
}
